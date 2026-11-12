package com.fortresstv.player.xtream

import java.io.IOException
import java.io.Reader

/**
 * Streaming reader for the large Xtream list endpoints (live streams, VOD, series, categories).
 *
 * The old path read the whole response into a String, parsed it into an org.json tree twice and
 * only then built model objects, so a 40 MB movie list needed several hundred MB of heap. This
 * reader walks the top-level array once and hands each element's top-level fields to the caller,
 * so only one small field map is alive at a time.
 *
 * Value conversion deliberately matches Android's bundled org.json (the runtime implementation),
 * so parsed catalogues are identical to what the tree-based parser produced:
 *  - number literals become Integer/Long/Double the way JSONTokener.readLiteral does, so
 *    optString(7.50) is "7.5" and optString(100) is "100";
 *  - optInt accepts numbers and numeric strings (via Double.parseDouble), like JSON.toInteger;
 *  - the tokenizer accepts the same lenient syntax (comments, single quotes, unquoted literals,
 *    ';' separators, '=' and '=>' key separators), so feeds Android accepted still parse.
 *
 * Nested objects/arrays inside an element are skipped. None of the list fields the app reads are
 * nested; a nested value now reads as absent instead of its serialized JSON text.
 */
internal class JsonFields(private val values: Map<String, Any?>) {
    fun has(key: String): Boolean = values.containsKey(key)

    /** Same result as Android JSONObject.optString(key). */
    fun optString(key: String): String = when (val value = values[key]) {
        null -> if (values.containsKey(key)) "null" else ""
        is String -> value
        NESTED -> ""
        else -> value.toString()
    }

    /** Same result as Android JSONObject.optInt(key, fallback). */
    fun optInt(key: String, fallback: Int): Int = when (val value = values[key]) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        // java.lang.Double.parseDouble (not Kotlin's stricter toDoubleOrNull) trims whitespace and
        // accepts the same forms as Android's JSON.toInteger.
        is String -> try {
            java.lang.Double.parseDouble(value).toInt()
        } catch (_: NumberFormatException) {
            fallback
        }
        else -> fallback
    }

    /** Matches XtreamClient's JSONObject.nullableString(): trimmed, blank and "null" become null. */
    fun nullableString(key: String): String? =
        optString(key).trim().takeIf { it.isNotEmpty() && !it.equals("null", true) }

    internal companion object {
        /** Marker for a nested object or array value that was skipped. */
        val NESTED = Any()
    }
}

internal object XtreamJsonStream {
    /**
     * Reads a top-level JSON array and calls [onObject] for every element that is an object,
     * with the element's index among object elements (non-object elements are skipped, exactly
     * like JSONArray.objects() did). Throws [IOException] for malformed input or a non-array root.
     */
    fun forEachObject(reader: Reader, onObject: (index: Int, fields: JsonFields) -> Unit) {
        val tokenizer = Tokenizer(reader)
        tokenizer.skipByteOrderMark()
        if (tokenizer.nextClean() != '['.code) throw tokenizer.syntaxError("Expected a JSON array")
        var objectIndex = 0
        if (tokenizer.nextClean() == ']'.code) return
        tokenizer.back()
        while (true) {
            when (val c = tokenizer.nextClean()) {
                ','.code, ';'.code -> {
                    // Android treats an empty slot as a null element.
                }
                ']'.code -> return
                else -> {
                    tokenizer.back()
                    if (c == '{'.code) {
                        tokenizer.nextClean()
                        onObject(objectIndex++, JsonFields(tokenizer.readObjectFields()))
                    } else {
                        tokenizer.skipValue()
                    }
                    when (tokenizer.nextClean()) {
                        ']'.code -> return
                        ','.code, ';'.code -> {}
                        else -> throw tokenizer.syntaxError("Unterminated array")
                    }
                    // A trailing separator before ']' is tolerated, like Android.
                    if (tokenizer.nextClean() == ']'.code) return
                    tokenizer.back()
                }
            }
        }
    }

    private class Tokenizer(private val reader: Reader) {
        private val buffer = CharArray(8192)
        private var length = 0
        private var position = 0
        private var pushedBack = false
        private var last = -1
        private var offset = 0L

        fun syntaxError(message: String) = IOException("$message at character $offset")

        /** Android's JSONTokener drops a leading U+FEFF. */
        fun skipByteOrderMark() {
            if (read() != 0xFEFF) back()
        }

        private fun read(): Int {
            if (pushedBack) {
                pushedBack = false
                return last
            }
            if (position >= length) {
                length = reader.read(buffer, 0, buffer.size)
                position = 0
                if (length <= 0) {
                    length = 0
                    last = -1
                    return -1
                }
            }
            last = buffer[position++].code
            offset++
            return last
        }

        fun back() {
            check(!pushedBack)
            pushedBack = true
        }

        /** Next non-whitespace, non-comment character, or -1 at end of input. */
        fun nextClean(): Int {
            while (true) {
                val c = read()
                when (c) {
                    '\t'.code, ' '.code, '\n'.code, '\r'.code -> continue
                    '/'.code -> {
                        val next = read()
                        when (next) {
                            '*'.code -> skipBlockComment()
                            '/'.code -> skipLine()
                            else -> {
                                if (next != -1) back()
                                return c
                            }
                        }
                    }
                    '#'.code -> skipLine()
                    else -> return c
                }
            }
        }

        private fun skipBlockComment() {
            var previous = -1
            while (true) {
                val c = read()
                if (c == -1) throw syntaxError("Unterminated comment")
                if (previous == '*'.code && c == '/'.code) return
                previous = c
            }
        }

        private fun skipLine() {
            while (true) {
                val c = read()
                if (c == -1 || c == '\n'.code || c == '\r'.code) return
            }
        }

        /** Reads the members of an object whose '{' was already consumed; nested values are skipped. */
        fun readObjectFields(): Map<String, Any?> {
            val fields = HashMap<String, Any?>()
            var first = nextClean()
            if (first == '}'.code) return fields
            back()
            while (true) {
                val keyStart = nextClean()
                val key = when (keyStart) {
                    '"'.code, '\''.code -> readString(keyStart)
                    -1 -> throw syntaxError("Unterminated object")
                    else -> {
                        back()
                        readLiteralText()
                    }
                }
                val separator = nextClean()
                if (separator != ':'.code && separator != '='.code) throw syntaxError("Expected ':' after $key")
                if (separator == '='.code) {
                    if (read() != '>'.code) back()
                }
                // Later duplicates replace earlier ones, as in Android's JSONObject.put.
                fields[key] = readValue(nested = false)
                when (nextClean()) {
                    '}'.code -> return fields
                    ','.code, ';'.code -> {}
                    else -> throw syntaxError("Unterminated object")
                }
                first = nextClean()
                if (first == '}'.code) return fields
                back()
            }
        }

        fun skipValue() {
            readValue(nested = true)
        }

        private fun readValue(nested: Boolean): Any? {
            val c = nextClean()
            return when (c) {
                -1 -> throw syntaxError("End of input")
                '{'.code -> {
                    skipContainer('}'.code)
                    JsonFields.NESTED
                }
                '['.code -> {
                    skipContainer(']'.code)
                    JsonFields.NESTED
                }
                '"'.code, '\''.code -> if (nested) {
                    skipString(c)
                    null
                } else readString(c)
                else -> {
                    back()
                    literalValue(readLiteralText())
                }
            }
        }

        /** Skips the rest of an object or array whose opening bracket was consumed. */
        private fun skipContainer(closing: Int) {
            var depth = 1
            val stack = ArrayList<Int>()
            var expected = closing
            while (depth > 0) {
                when (val c = nextClean()) {
                    -1 -> throw syntaxError("Unterminated nested value")
                    '"'.code, '\''.code -> skipString(c)
                    '{'.code -> {
                        stack += expected
                        expected = '}'.code
                        depth++
                    }
                    '['.code -> {
                        stack += expected
                        expected = ']'.code
                        depth++
                    }
                    '}'.code, ']'.code -> {
                        if (c != expected) throw syntaxError("Mismatched bracket")
                        depth--
                        if (stack.isNotEmpty()) expected = stack.removeAt(stack.lastIndex)
                    }
                    else -> {}
                }
            }
        }

        private fun skipString(quote: Int) {
            while (true) {
                val c = read()
                when (c) {
                    -1 -> throw syntaxError("Unterminated string")
                    '\\'.code -> if (read() == -1) throw syntaxError("Unterminated escape")
                    quote -> return
                }
            }
        }

        /** Same escapes as Android's JSONTokener.nextString / readEscapeCharacter. */
        private fun readString(quote: Int): String {
            val builder = StringBuilder()
            while (true) {
                val c = read()
                when (c) {
                    -1 -> throw syntaxError("Unterminated string")
                    quote -> return builder.toString()
                    '\\'.code -> {
                        when (val escaped = read()) {
                            -1 -> throw syntaxError("Unterminated escape sequence")
                            'u'.code -> {
                                var value = 0
                                repeat(4) {
                                    val digit = Character.digit(read(), 16)
                                    if (digit < 0) throw syntaxError("Invalid escape sequence")
                                    value = value * 16 + digit
                                }
                                builder.append(value.toChar())
                            }
                            't'.code -> builder.append('\t')
                            'b'.code -> builder.append('\b')
                            'n'.code -> builder.append('\n')
                            'r'.code -> builder.append('\r')
                            'f'.code -> builder.append('\u000C')
                            else -> builder.append(escaped.toChar())
                        }
                    }
                    else -> builder.append(c.toChar())
                }
            }
        }

        /** Unquoted text up to the next delimiter, as Android's JSONTokener.nextToInternal("{}[]/\\:,=;# \t\f"). */
        private fun readLiteralText(): String {
            val builder = StringBuilder()
            while (true) {
                val c = read()
                if (c == -1 || c == '\r'.code || c == '\n'.code || LITERAL_DELIMITERS.indexOf(c.toChar()) >= 0) {
                    if (c != -1) back()
                    val text = builder.toString().trim()
                    if (text.isEmpty()) throw syntaxError("Expected literal value")
                    return text
                }
                builder.append(c.toChar())
            }
        }

        /** Same conversion as Android's JSONTokener.readLiteral. */
        private fun literalValue(literal: String): Any? {
            if (literal.equals("null", ignoreCase = true)) return null
            if (literal.equals("true", ignoreCase = true)) return true
            if (literal.equals("false", ignoreCase = true)) return false
            if (literal.indexOf('.') == -1) {
                var base = 10
                var number = literal
                if (number.startsWith("0x") || number.startsWith("0X")) {
                    number = number.substring(2)
                    base = 16
                } else if (number.startsWith("0") && number.length > 1) {
                    number = number.substring(1)
                    base = 8
                }
                try {
                    val longValue = java.lang.Long.parseLong(number, base)
                    return if (longValue <= Int.MAX_VALUE && longValue >= Int.MIN_VALUE) longValue.toInt() else longValue
                } catch (_: NumberFormatException) {
                    // Too large, exponent form or an unquoted string: try floating point next.
                }
            }
            return try {
                java.lang.Double.valueOf(literal)
            } catch (_: NumberFormatException) {
                literal
            }
        }

        private companion object {
            const val LITERAL_DELIMITERS = "{}[]/\\:,=;# \t\u000C"
        }
    }
}

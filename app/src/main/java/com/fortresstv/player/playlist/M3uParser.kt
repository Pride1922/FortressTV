package com.fortresstv.player.playlist

object M3uParser {
    private const val MAX_CHANNELS = 250_000
    private const val MAX_LINE_CHARS = 1024 * 1024
    private val attributePattern = Regex("""([\w-]+)\s*=\s*"([^"]*)"""")

    fun parse(content: String): Playlist = parse(content.reader())

    fun parse(reader: java.io.Reader): Playlist {
        val channels = mutableListOf<PlaylistChannel>()
        var pendingInfo: ChannelInfo? = null
        var pendingGroup: String? = null

        val lines = (reader as? java.io.BufferedReader ?: reader.buffered()).lineSequence()
        lines.forEach { rawLine ->
            require(rawLine.length <= MAX_LINE_CHARS) { "Playlist line exceeds the 1 MB safety limit" }
            val line = rawLine.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pendingInfo = parseInfo(line)
                    pendingGroup = null
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    pendingGroup = line.substringAfter(':').trim().ifEmpty { null }
                }

                line.isNotEmpty() && !line.startsWith('#') -> {
                    require(channels.size < MAX_CHANNELS) { "Playlist exceeds the 250,000 channel safety limit" }
                    val info = pendingInfo ?: ChannelInfo(name = line)
                    val channel = PlaylistChannel(
                        position = channels.size,
                        name = info.name.ifBlank { info.tvgName ?: "Channel ${channels.size + 1}" },
                        streamUrl = line,
                        tvgId = info.tvgId,
                        tvgName = info.tvgName,
                        logoUrl = info.logoUrl,
                        group = pendingGroup ?: info.group
                    )
                    channels += channel.copy(sourceItemId = channel.stableSourceItemId())
                    pendingInfo = null
                    pendingGroup = null
                }
            }
        }

        return Playlist(channels)
    }

    private fun parseInfo(line: String): ChannelInfo {
        val body = line.substringAfter(':')
        val separator = findNameSeparator(body)
        val metadata = if (separator >= 0) body.substring(0, separator) else body
        val name = if (separator >= 0) body.substring(separator + 1).trim() else ""
        val attributes = attributePattern.findAll(metadata).associate {
            it.groupValues[1].lowercase() to it.groupValues[2].trim()
        }

        return ChannelInfo(
            name = name,
            tvgId = attributes["tvg-id"].nullIfBlank(),
            tvgName = attributes["tvg-name"].nullIfBlank(),
            logoUrl = attributes["tvg-logo"].nullIfBlank(),
            group = attributes["group-title"].nullIfBlank()
        )
    }

    private fun findNameSeparator(value: String): Int {
        var inQuotes = false
        value.forEachIndexed { index, character ->
            when (character) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return index
            }
        }
        return -1
    }

    private fun String?.nullIfBlank(): String? = this?.ifBlank { null }

    private data class ChannelInfo(
        val name: String,
        val tvgId: String? = null,
        val tvgName: String? = null,
        val logoUrl: String? = null,
        val group: String? = null
    )
}

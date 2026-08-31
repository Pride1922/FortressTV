package com.fortresstv.player.epg

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import javax.xml.parsers.SAXParserFactory

object XmlTvParser {
    const val DEFAULT_LOADED_DAYS = 3

    fun parse(
        input: InputStream,
        referenceTimeMillis: Long = System.currentTimeMillis(),
        loadedDays: Int = DEFAULT_LOADED_DAYS
    ): EpgGuide {
        val handler = XmlTvHandler(referenceTimeMillis, loadedDays.coerceIn(1, 7))
        SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
        }.newSAXParser().parse(input, handler)
        return handler.guide()
    }

    private class XmlTvHandler(
        private val referenceTime: Long,
        private val loadedDays: Int
    ) : DefaultHandler() {
        private val pastCutoff = if (referenceTime > 0L) referenceTime - (6 * 3600_000L) else Long.MIN_VALUE
        private val futureCutoff = if (referenceTime > 0L) {
            val zone = ZoneId.systemDefault()
            Instant.ofEpochMilli(referenceTime)
                .atZone(zone)
                .toLocalDate()
                .plusDays(loadedDays.toLong())
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        } else {
            Long.MAX_VALUE
        }

        private val channels = linkedMapOf<String, EpgChannel>()
        private val programmes = linkedMapOf<String, MutableList<EpgProgramme>>()
        private val text = StringBuilder()
        private var channel: MutableChannel? = null
        private var programme: MutableProgramme? = null

        override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
            text.setLength(0)
            when (qName.lowercase()) {
                "channel" -> channel = MutableChannel(id = attributes.getValue("id").orEmpty())
                "programme" -> programme = MutableProgramme(
                    channelId = attributes.getValue("channel").orEmpty(),
                    start = attributes.getValue("start").orEmpty(),
                    stop = attributes.getValue("stop")
                )
                "icon" -> {
                    val source = attributes.getValue("src")?.ifBlank { null }
                    if (channel != null) channel?.iconUrl = source
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            text.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            val value = text.toString().trim().ifBlank { null }
            when (qName.lowercase()) {
                "display-name" -> if (value != null) channel?.displayNames?.add(value)
                "channel" -> channel?.takeIf { it.id.isNotBlank() }?.let {
                    channels[it.id] = EpgChannel(
                        id = it.id,
                        displayName = it.displayNames.firstOrNull(),
                        iconUrl = it.iconUrl,
                        displayNames = it.displayNames.toList()
                    )
                }.also { channel = null }
                "title" -> programme?.title = value
                "desc" -> programme?.description = value
                "category" -> if (programme?.category == null) programme?.category = value
                "programme" -> programme?.takeIf {
                    it.channelId.isNotBlank() && it.start.isNotBlank() && !it.title.isNullOrBlank()
                }?.let {
                    val startEpoch = XmlTvTime.parseToEpoch(it.start) ?: 0L
                    val stopEpoch = it.stop?.let { s -> XmlTvTime.parseToEpoch(s) }
                    val isObsolete = (stopEpoch != null && stopEpoch < pastCutoff) || (startEpoch > 0L && startEpoch < pastCutoff && stopEpoch == null)
                    val isTooFarFuture = startEpoch > futureCutoff

                    if (!isObsolete && !isTooFarFuture) {
                        programmes.getOrPut(it.channelId) { mutableListOf() } += EpgProgramme(
                            channelId = it.channelId,
                            title = it.title.orEmpty(),
                            description = it.description,
                            category = it.category,
                            startEpochMilli = startEpoch,
                            stopEpochMilli = stopEpoch
                        )
                    }
                }.also { programme = null }
            }
            text.setLength(0)
        }

        fun guide() = EpgGuide(
            channels = channels,
            programmes = programmes.mapValues { (_, entries) -> entries.sortedBy { it.startEpochMilli } }
        )

        private data class MutableChannel(
            val id: String,
            val displayNames: MutableList<String> = mutableListOf(),
            var iconUrl: String? = null
        )

        private data class MutableProgramme(
            val channelId: String,
            val start: String,
            val stop: String?,
            var title: String? = null,
            var description: String? = null,
            var category: String? = null
        )
    }
}

package com.fortresstv.player.epg

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

object XmlTvParser {
    fun parse(input: InputStream): EpgGuide {
        val handler = XmlTvHandler()
        SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
        }.newSAXParser().parse(input, handler)
        return handler.guide()
    }

    private class XmlTvHandler : DefaultHandler() {
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
                "display-name" -> if (channel?.displayName == null) channel?.displayName = value
                "channel" -> channel?.takeIf { it.id.isNotBlank() }?.let {
                    channels[it.id] = EpgChannel(it.id, it.displayName, it.iconUrl)
                }.also { channel = null }
                "title" -> programme?.title = value
                "desc" -> programme?.description = value
                "category" -> if (programme?.category == null) programme?.category = value
                "programme" -> programme?.takeIf {
                    it.channelId.isNotBlank() && it.start.isNotBlank() && !it.title.isNullOrBlank()
                }?.let {
                    programmes.getOrPut(it.channelId) { mutableListOf() } += EpgProgramme(
                        channelId = it.channelId,
                        start = it.start,
                        stop = it.stop,
                        title = it.title.orEmpty(),
                        description = it.description,
                        category = it.category
                    )
                }.also { programme = null }
            }
            text.setLength(0)
        }

        fun guide() = EpgGuide(
            channels = channels,
            programmes = programmes.mapValues { (_, entries) -> entries.toList() }
        )

        private data class MutableChannel(
            val id: String,
            var displayName: String? = null,
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

package com.fortresstv.player.epg

import android.content.Context
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

object XmlTvParser {
    const val DEFAULT_LOADED_DAYS = 3

    fun parse(
        input: InputStream,
        referenceTimeMillis: Long = System.currentTimeMillis(),
        loadedDays: Int = DEFAULT_LOADED_DAYS
    ): EpgGuide {
        val handler = InMemXmlTvHandler(referenceTimeMillis, loadedDays.coerceIn(1, 7))
        secureSaxParserFactory().newSAXParser().parse(input, handler)
        return handler.guide()
    }

    fun parseToDisk(
        input: InputStream,
        context: Context,
        fingerprint: String,
        referenceTimeMillis: Long = System.currentTimeMillis(),
        loadedDays: Int = DEFAULT_LOADED_DAYS,
        timezoneId: String = ZoneId.systemDefault().id
    ): EpgManifest {
        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val today = Instant.ofEpochMilli(referenceTimeMillis).atZone(zone).toLocalDate()
        val todayEpochDay = today.toEpochDay()
        val genId = UUID.randomUUID().toString().replace("-", "")

        val session = EpgDiskCache.createStagingSession(
            context = context,
            generationId = genId,
            fingerprint = fingerprint,
            timezoneId = zone.id,
            loadedDays = loadedDays.coerceIn(1, 7),
            todayEpochDay = todayEpochDay
        )

        try {
            val handler = StreamingXmlTvHandler(
                stagingSession = session,
                referenceTime = referenceTimeMillis,
                loadedDays = loadedDays.coerceIn(1, 7),
                zone = zone
            )
            secureSaxParserFactory().newSAXParser().parse(input, handler)

            return session.commit(handler.channels)
        } catch (t: Throwable) {
            session.abort()
            throw t
        }
    }

    private fun secureSaxParserFactory(): SAXParserFactory = SAXParserFactory.newInstance().apply {
        isNamespaceAware = false
        isXIncludeAware = false
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }

    private class StreamingXmlTvHandler(
        private val stagingSession: EpgDiskCache.StagingSession,
        private val referenceTime: Long,
        private val loadedDays: Int,
        private val zone: ZoneId
    ) : DefaultHandler() {
        val channels = linkedMapOf<String, EpgChannel>()
        private val pastCutoff = if (referenceTime > 0L) referenceTime - (6 * 3600_000L) else Long.MIN_VALUE
        private val futureCutoff = if (referenceTime > 0L) {
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
        private val todayEpochDay = Instant.ofEpochMilli(referenceTime).atZone(zone).toLocalDate().toEpochDay()
        private val maxEpochDay = todayEpochDay + loadedDays - 1

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
                    val effectiveStop = stopEpoch ?: (startEpoch + 30 * 60_000L)
                    val isObsolete = (effectiveStop < pastCutoff) || (startEpoch > 0L && startEpoch < pastCutoff && stopEpoch == null)
                    val isTooFarFuture = startEpoch >= futureCutoff

                    if (!isObsolete && !isTooFarFuture && startEpoch > 0L) {
                        val prog = EpgProgramme(
                            channelId = it.channelId,
                            title = it.title.orEmpty(),
                            description = it.description,
                            category = it.category,
                            startEpochMilli = startEpoch,
                            stopEpochMilli = stopEpoch
                        )

                        // Calculate intersecting epoch days to support midnight crossing
                        val startDay = Instant.ofEpochMilli(startEpoch).atZone(zone).toLocalDate().toEpochDay()
                        val stopDay = Instant.ofEpochMilli(effectiveStop.coerceAtLeast(startEpoch + 1) - 1L)
                            .atZone(zone).toLocalDate().toEpochDay()

                        for (day in startDay..stopDay) {
                            if (day in todayEpochDay..maxEpochDay) {
                                stagingSession.appendProgramme(day, prog)
                            }
                        }
                    }
                }.also { programme = null }
            }
            text.setLength(0)
        }
    }

    private class InMemXmlTvHandler(
        private val referenceTime: Long,
        private val loadedDays: Int
    ) : DefaultHandler() {
        private val zone = ZoneId.systemDefault()
        private val pastCutoff = if (referenceTime > 0L) referenceTime - (6 * 3600_000L) else Long.MIN_VALUE
        private val futureCutoff = if (referenceTime > 0L) {
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
                    val effectiveStop = stopEpoch ?: (startEpoch + 30 * 60_000L)
                    val isObsolete = (effectiveStop < pastCutoff) || (startEpoch > 0L && startEpoch < pastCutoff && stopEpoch == null)
                    val isTooFarFuture = startEpoch >= futureCutoff

                    if (!isObsolete && !isTooFarFuture && startEpoch > 0L) {
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
    }

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

package com.fortresstv.player.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XmlTvParserTest {
    @Test
    fun parsesChannelAndProgrammeByExactId() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="RTP1.pt">
                <display-name>RTP 1</display-name>
                <icon src="https://i.imgur.com/w7GV86i.png"/>
                <url>https://meo.pt</url>
              </channel>
              <programme start="20260829120000 +0000" stop="20260829130000 +0000" channel="RTP1.pt">
                <title lang="pt">Jornal da Tarde</title>
                <desc lang="pt">Notícias do dia.</desc>
                <category>News</category>
              </programme>
            </tv>
        """.trimIndent()

        val guide = XmlTvParser.parse(
            xml.byteInputStream(),
            referenceTimeMillis = XmlTvTime.parseToEpoch("20260829123000 +0000")!!
        )

        assertEquals("RTP 1", guide.channels.getValue("RTP1.pt").displayName)
        assertEquals("https://i.imgur.com/w7GV86i.png", guide.channels.getValue("RTP1.pt").iconUrl)
        assertEquals("Jornal da Tarde", guide.programmes.getValue("RTP1.pt").single().title)
        assertNull(guide.channels["rtp1.pt"])
    }

    @Test
    fun retainsThreeCalendarDaysAndDropsLaterProgrammes() {
        val xml = """
            <tv>
              <channel id="RTP1.pt"><display-name>RTP 1</display-name></channel>
              <programme start="20260831120000 +0000" stop="20260831130000 +0000" channel="RTP1.pt">
                <title>Third calendar day</title>
              </programme>
              <programme start="20260901120000 +0000" stop="20260901130000 +0000" channel="RTP1.pt">
                <title>Fourth calendar day</title>
              </programme>
            </tv>
        """.trimIndent()

        val guide = XmlTvParser.parse(
            xml.byteInputStream(),
            referenceTimeMillis = XmlTvTime.parseToEpoch("20260829120000 +0000")!!
        )

        assertEquals(listOf("Third calendar day"), guide.programmes.getValue("RTP1.pt").map { it.title })
    }
}

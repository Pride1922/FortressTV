package com.fortresstv.player.epg

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.xml.sax.SAXException
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class XmlTvAndroidParserTest {
    private val xml = """<tv>
        <channel id="test"><display-name>Test channel</display-name></channel>
        <programme channel="test" start="20260910120000 +0000" stop="20260910130000 +0000">
            <title>Test programme</title>
        </programme>
    </tv>"""
    private val referenceTime = 1789041600000L // 2026-09-10 12:00 UTC

    @Test
    fun parsesUsingAndroidSaxImplementation() {
        val guide = XmlTvParser.parse(xml.byteInputStream(), referenceTime)
        assertEquals("Test channel", guide.channels.getValue("test").displayName)
        assertEquals("Test programme", guide.programmes.getValue("test").single().title)
    }

    @Test
    fun parsesFreshGuideToDiskUsingAndroidSaxImplementation() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(appContext.cacheDir.toPath(), "xmltv-test-").toFile()
        val context = object : ContextWrapper(appContext) {
            override fun getFilesDir() = directory
        }
        try {
            val manifest = XmlTvParser.parseToDisk(
                xml.byteInputStream(), context, "test", referenceTime, timezoneId = "UTC"
            )
            assertEquals("Test channel", manifest.channels.getValue("test").displayName)
            val day = EpgDiskCache.loadDayPartition(context, manifest, manifest.todayEpochDay)!!
            assertEquals("Test programme", day.programmes.getValue("test").single().title)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsDtdsOnAndroid() {
        for (declaration in listOf(
            "<!DOCTYPE tv [<!ENTITY title 'Expanded title'>]>",
            "<!DOCTYPE tv SYSTEM 'https://example.invalid/never-fetch.dtd'>"
        )) {
            assertThrows(SAXException::class.java) {
                XmlTvParser.parse((declaration + xml).byteInputStream(), referenceTime)
            }
        }
    }
}

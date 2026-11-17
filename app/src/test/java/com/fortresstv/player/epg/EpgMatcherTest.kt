package com.fortresstv.player.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EpgMatcherTest {

    @Test
    fun normalizesChannelNamesCorrectly() {
        assertEquals("s+", EpgMatcher.normalize("PT: S+"))
        assertEquals("s+", EpgMatcher.normalize("PT | S+"))
        assertEquals("s+", EpgMatcher.normalize("[PT] S+ (HD)"))
        assertEquals("s+", EpgMatcher.normalize("PT: S+ FHD"))
        assertEquals("s+", EpgMatcher.normalize("S+"))

        assertEquals("rtp1", EpgMatcher.normalize("PT: RTP 1 HD"))
        assertEquals("rtp1", EpgMatcher.normalize("PT | RTP 1 (1080p)"))
        assertEquals("rtp1", EpgMatcher.normalize("RTP 1"))

        assertEquals("sporttv1", EpgMatcher.normalize("PT: SPORT TV 1 FHD"))
        assertEquals("sporttv1", EpgMatcher.normalize("PT | Sport TV 1 [HEVC]"))
        assertEquals("sporttv1", EpgMatcher.normalize("Sport TV 1"))

        assertEquals("sicnoticias", EpgMatcher.normalize("PT: SIC Notícias HD"))
        assertEquals("sicnoticias", EpgMatcher.normalize("SIC Noticias"))
    }

    @Test
    fun epgGuideResolvesChannelWithoutTvgIdBySmartNameMatch() {
        val guide = EpgGuide(
            channels = mapOf(
                "SPlus.pt" to EpgChannel("SPlus.pt", "S+"),
                "RTP1.pt" to EpgChannel("RTP1.pt", "RTP 1"),
                "SIC.pt" to EpgChannel("SIC.pt", "SIC"),
                "SportTV1.pt" to EpgChannel("SportTV1.pt", "Sport TV 1", displayNames = listOf("Sport TV 1", "SPORT TV 1")),
                "CNNPortugal.pt" to EpgChannel("CNNPortugal.pt", "CNN Portugal")
            ),
            programmes = emptyMap()
        )

        // Matches channel "PT: S+" without tvgId
        val sPlusId = guide.resolveChannelId(tvgId = null, name = "PT: S+ | Group: PT | GENERALISTA")
        // Wait, "PT: S+ | Group: PT | GENERALISTA" has " | Group: ..." in user's example?
        // Let's test standard channel.name which is "PT: S+"
        assertEquals("SPlus.pt", guide.resolveChannelId(tvgId = null, name = "PT: S+"))
        assertEquals("SPlus.pt", guide.resolveChannelId(tvgId = "", name = "PT: S+ FHD"))
        assertEquals("SPlus.pt", guide.resolveChannelId(tvgId = "No tvg-id", name = "PT | S+ (HD)"))

        // Matches RTP 1 without tvgId
        assertEquals("RTP1.pt", guide.resolveChannelId(tvgId = null, name = "PT: RTP 1 HD"))

        // Matches Sport TV 1
        assertEquals("SportTV1.pt", guide.resolveChannelId(tvgId = null, name = "PT: SPORT TV 1 FHD"))

        // Matches by exact tvgId when available
        assertEquals("CNNPortugal.pt", guide.resolveChannelId(tvgId = "CNNPortugal.pt", name = "Any Name"))

        // Unmatched channel returns null
        assertNull(guide.resolveChannelId(tvgId = null, name = "Random Nonexistent Channel"))
    }

    @Test
    fun epgGuideResolvesNumberedChannelsWithoutCollisions() {
        val guide = EpgGuide(
            channels = mapOf(
                "DAZN1.pt" to EpgChannel("DAZN1.pt", "DAZN 1", displayNames = listOf("DAZN 1", "DAZN1")),
                "DAZN2.pt" to EpgChannel("DAZN2.pt", "DAZN 2", displayNames = listOf("DAZN 2", "DAZN2")),
                "DAZN3.pt" to EpgChannel("DAZN3.pt", "DAZN 3", displayNames = listOf("DAZN 3", "DAZN3")),
                "DAZN4.pt" to EpgChannel("DAZN4.pt", "DAZN 4", displayNames = listOf("DAZN 4", "DAZN4")),
                "DAZN5.pt" to EpgChannel("DAZN5.pt", "DAZN 5", displayNames = listOf("DAZN 5", "DAZN5")),
                "DAZN6.pt" to EpgChannel("DAZN6.pt", "DAZN 6", displayNames = listOf("DAZN 6", "DAZN6"))
            ),
            programmes = emptyMap()
        )

        // Even if IPTV provider assigned generic tvgId = "DAZN" to all DAZN channels, each channel must match its own number!
        assertEquals("DAZN1.pt", guide.resolveChannelId(tvgId = "DAZN", name = "PT: DAZN 1 HD"))
        assertEquals("DAZN2.pt", guide.resolveChannelId(tvgId = "DAZN", name = "PT: DAZN 2 HD"))
        assertEquals("DAZN3.pt", guide.resolveChannelId(tvgId = "DAZN", name = "PT: DAZN 3 HD"))
        assertEquals("DAZN4.pt", guide.resolveChannelId(tvgId = "DAZN", name = "PT: DAZN 4 HD"))
        assertEquals("DAZN5.pt", guide.resolveChannelId(tvgId = "DAZN", name = "PT: DAZN 5 HD"))
        assertEquals("DAZN6.pt", guide.resolveChannelId(tvgId = "DAZN", name = "PT: DAZN 6 HD"))

        // Without tvgId at all
        assertEquals("DAZN2.pt", guide.resolveChannelId(tvgId = null, name = "PT: DAZN 2 FHD"))
        assertEquals("DAZN4.pt", guide.resolveChannelId(tvgId = null, name = "PT: DAZN 4 FHD"))
    }

    @Test
    fun normalizesMultiSourcePortuguesePrefixesSuperscriptsAndSuffixes() {
        assertEquals("rtp1", EpgMatcher.normalize("|PT| RTP 1"))
        assertEquals("sporttv1", EpgMatcher.normalize("PT | FHD | Sport TV 1"))
        assertEquals("rtp1", EpgMatcher.normalize("RTP 1 ᴴᴰ"))
        assertEquals("sporttv1", EpgMatcher.normalize("Sport TV 1 ᶠᴴᴰ"))
        assertEquals("rtp1", EpgMatcher.normalize("RTP 1 PT"))
        assertEquals("cnnportugal", EpgMatcher.normalize("PT ◉ CNN Portugal"))
    }

    @Test
    fun resolveChannelIdFallsBackToTvgNameAndCrossSourceManualMapping() {
        val guide = EpgGuide(
            channels = mapOf(
                "RTP1.pt" to EpgChannel("RTP1.pt", "RTP 1"),
                "SportTV1.pt" to EpgChannel("SportTV1.pt", "Sport TV 1"),
                "SportTV2.pt" to EpgChannel("SportTV2.pt", "Sport TV 2")
            ),
            programmes = emptyMap()
        )

        // When display name is non-standard but tvgName matches
        assertEquals(
            "RTP1.pt",
            guide.resolveChannelId(tvgId = null, name = "Canal Um", tvgName = "|PT| RTP 1 ᴴᴰ")
        )

        // Cross-source manual mapping reuse by normalized name + digits
        val mappings = mapOf(
            "src1:ch1" to EpgChannelMapping(
                iptvSourceId = "src1",
                iptvChannelId = "ch1",
                epgSourceId = "pt-epg",
                xmltvChannelId = "SportTV1.pt",
                channelName = "PT: Sport TV 1 FHD"
            )
        )
        assertEquals(
            "SportTV1.pt",
            mappings.resolveManualXmltvId("src2", "other-id", "|PT| Sport TV 1 ᴴᴰ")
        )
        assertNull(
            mappings.resolveManualXmltvId("src2", "other-id-2", "|PT| Sport TV 2 ᴴᴰ")
        )
    }

    @Test
    fun resolvesPortugueseRebrandAndTrailingSeparatorAliases() {
        val guide = EpgGuide(
            channels = mapOf(
                "FOX.pt" to EpgChannel("FOX.pt", "FOX"),
                "FOXLife.pt" to EpgChannel("FOXLife.pt", "FOX Life"),
                "Eleven1.pt" to EpgChannel("Eleven1.pt", "Eleven Sports 1"),
                "TLC.pt" to EpgChannel("TLC.pt", "TLC"),
                "RTP1.pt" to EpgChannel("RTP1.pt", "RTP 1")
            ),
            programmes = emptyMap()
        )

        assertEquals("FOX.pt", guide.resolveChannelId(null, "PT: Star Channel HD"))
        assertEquals("FOXLife.pt", guide.resolveChannelId(null, "PT | Star Life FHD"))
        assertEquals("Eleven1.pt", guide.resolveChannelId(null, "PT: DAZN 1 HD"))
        assertEquals("TLC.pt", guide.resolveChannelId(null, "TLC Portugal"))
        assertEquals("RTP1.pt", guide.resolveChannelId(null, "RTP 1 | PT"))
        assertEquals("RTP1.pt", guide.resolveChannelId(null, "RTP 1 - PT"))
    }
}

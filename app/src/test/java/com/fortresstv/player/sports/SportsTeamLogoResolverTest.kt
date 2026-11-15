package com.fortresstv.player.sports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SportsTeamLogoResolverTest {

    @Test
    fun `buildSearchCandidates strips club prefixes and accents`() {
        val candidates = SportsTeamLogoResolver.buildSearchCandidates("UD Oliveirense")
        assertTrue("UD Oliveirense" in candidates)
        assertTrue("Oliveirense" in candidates)

        val accented = SportsTeamLogoResolver.buildSearchCandidates("Académico de Viseu")
        assertTrue("Académico de Viseu" in accented)
        assertTrue("Academico de Viseu" in accented)
    }

    @Test
    fun `cacheKey normalizes accents and casing`() {
        assertEquals(
            SportsTeamLogoResolver.cacheKey("Lusitânia Lourosa"),
            SportsTeamLogoResolver.cacheKey("lusitania  lourosa")
        )
        assertNull(SportsTeamLogoResolver.cacheKey("   "))
    }

    @Test
    fun `parseBestBadgeFromJson filters by sport and prefers exact match`() {
        val json = """
            {
              "teams": [
                {
                  "strTeam": "Penafiel Rugby",
                  "strSport": "Rugby",
                  "strBadge": "https://example.com/rugby.png"
                },
                {
                  "strTeam": "FC Penafiel",
                  "strTeamAlternate": "Penafiel",
                  "strSport": "Soccer",
                  "strBadge": "https://example.com/penafiel.png"
                },
                {
                  "strTeam": "Penafiel B",
                  "strSport": "Soccer",
                  "strBadge": "https://example.com/penafiel-b.png"
                }
              ]
            }
        """.trimIndent()

        val badge = SportsTeamLogoResolver.parseBestBadgeFromJson(
            rawJson = json,
            queryTeamName = "Penafiel",
            sport = SportsKind.FOOTBALL
        )
        assertEquals("https://example.com/penafiel.png", badge)
    }

    @Test
    fun `parseBestBadgeFromJson skips blank or null badges`() {
        val json = """
            {
              "teams": [
                {
                  "strTeam": "Vizela",
                  "strSport": "Soccer",
                  "strBadge": ""
                },
                {
                  "strTeam": "Vizela",
                  "strSport": "Soccer",
                  "strBadge": "https://example.com/vizela.png"
                }
              ]
            }
        """.trimIndent()

        val badge = SportsTeamLogoResolver.parseBestBadgeFromJson(
            rawJson = json,
            queryTeamName = "FC Vizela",
            sport = SportsKind.FOOTBALL
        )
        assertEquals("https://example.com/vizela.png", badge)
    }

    @Test
    fun `competitionLogoFallback resolves Liga Portugal 2 and Primeira Liga`() {
        assertEquals(
            "https://r2.thesportsdb.com/images/media/league/badge/pns32l1782689191.png",
            SportsTeamLogoResolver.competitionLogoFallback("Liga Portugal 2 Meu Super")
        )
        assertEquals(
            "https://r2.thesportsdb.com/images/media/league/badge/pns32l1782689191.png",
            SportsTeamLogoResolver.competitionLogoFallback("Segunda Liga")
        )
        assertEquals(
            "https://r2.thesportsdb.com/images/media/league/badge/vuytsr1549891157.png",
            SportsTeamLogoResolver.competitionLogoFallback("Liga Portugal Betclic")
        )
        assertNull(SportsTeamLogoResolver.competitionLogoFallback("Unknown Cup"))
    }
}

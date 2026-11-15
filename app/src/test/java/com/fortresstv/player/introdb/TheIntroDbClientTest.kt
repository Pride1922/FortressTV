package com.fortresstv.player.introdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TheIntroDbClientTest {

    private val sampleJsonResponse = """
        {
          "tvdb_id": 81189,
          "type": "tv",
          "season": 1,
          "episode": 1,
          "recap": [
            {
              "start_ms": 1000,
              "end_ms": 30000
            }
          ],
          "intro": [
            {
              "start_ms": 228671,
              "end_ms": 246465
            }
          ],
          "credits": [
            {
              "start_ms": 3432825,
              "end_ms": null
            }
          ],
          "preview": []
        }
    """.trimIndent()

    @Test
    fun testParseResponseJson() {
        val client = TheIntroDbClient()
        val segments = client.parseResponseJson(sampleJsonResponse, season = 1, episode = 1)

        assertNotNull(segments)
        assertEquals(1, segments!!.season)
        assertEquals(1, segments.episode)

        // Recap
        assertEquals(1, segments.recap.size)
        assertEquals(1000L, segments.recap[0].startMs)
        assertEquals(30000L, segments.recap[0].endMs)

        // Intro
        assertEquals(1, segments.intro.size)
        assertEquals(228671L, segments.intro[0].startMs)
        assertEquals(246465L, segments.intro[0].endMs)

        // Credits
        assertEquals(1, segments.credits.size)
        assertEquals(3432825L, segments.credits[0].startMs)
        assertNull(segments.credits[0].endMs)

        // Preview
        assertTrue(segments.preview.isEmpty())
    }

    @Test
    fun testActiveSegmentDetection() {
        val client = TheIntroDbClient()
        val segments = client.parseResponseJson(sampleJsonResponse, season = 1, episode = 1)!!
        val totalDurationMs = 3600000L

        // Before recap
        assertNull(segments.activeSegment(500L, totalDurationMs))

        // During recap
        val activeRecap = segments.activeSegment(15000L, totalDurationMs)
        assertNotNull(activeRecap)
        assertEquals(SegmentType.RECAP, activeRecap!!.type)
        assertEquals(30000L, activeRecap.endMs)

        // After recap, before intro
        assertNull(segments.activeSegment(100000L, totalDurationMs))

        // During intro
        val activeIntro = segments.activeSegment(235000L, totalDurationMs)
        assertNotNull(activeIntro)
        assertEquals(SegmentType.INTRO, activeIntro!!.type)
        assertEquals(246465L, activeIntro.endMs)

        // During main show
        assertNull(segments.activeSegment(1500000L, totalDurationMs))

        // During credits (where end_ms is null)
        val activeCredits = segments.activeSegment(3450000L, totalDurationMs)
        assertNotNull(activeCredits)
        assertEquals(SegmentType.CREDITS, activeCredits!!.type)
        assertNull(activeCredits.endMs)
    }

    @Test
    fun testMalformedJsonHandling() {
        val client = TheIntroDbClient()
        val segments = client.parseResponseJson("{ invalid json }", 1, 1)
        assertNull(segments)
    }

    @Test
    fun testSegmentWithInvalidRangeIgnored() {
        val json = """
            {
              "season": 2,
              "episode": 3,
              "intro": [
                {
                  "start_ms": 50000,
                  "end_ms": 20000
                }
              ]
            }
        """.trimIndent()
        val client = TheIntroDbClient()
        val segments = client.parseResponseJson(json, 2, 3)
        assertNotNull(segments)
        assertTrue(segments!!.intro.isEmpty())
    }

    @Test
    fun testNullStartMsIntroAndNoSegmentCredits() {
        val json = """
            {
              "tvdb_id": 71663,
              "type": "tv",
              "season": 1,
              "episode": 1,
              "intro": [
                {
                  "start_ms": null,
                  "end_ms": 24725
                }
              ],
              "credits": [
                {
                  "start_ms": 0,
                  "end_ms": null
                },
                {
                  "start_ms": null,
                  "end_ms": null
                }
              ]
            }
        """.trimIndent()
        val client = TheIntroDbClient()
        val segments = client.parseResponseJson(json, 1, 1)
        assertNotNull(segments)
        assertEquals(1, segments!!.intro.size)
        assertEquals(0L, segments.intro[0].startMs)
        assertEquals(24725L, segments.intro[0].endMs)
        assertTrue(segments.credits.isEmpty())

        val activeAtStart = segments.activeSegment(1000L, 1_400_000L)
        assertNotNull(activeAtStart)
        assertEquals(SegmentType.INTRO, activeAtStart!!.type)
    }

    @Test
    fun testMovieResponseParsing() {
        val json = """
            {
              "tvdb_id": 113,
              "type": "movie",
              "intro": [
                {
                  "start_ms": null,
                  "end_ms": 38000
                }
              ],
              "credits": [
                {
                  "start_ms": 5800000,
                  "end_ms": null
                }
              ]
            }
        """.trimIndent()
        val client = TheIntroDbClient()
        val segments = client.parseResponseJson(json, 0, 0)
        assertNotNull(segments)
        assertEquals(1, segments!!.intro.size)
        assertEquals(0L, segments.intro[0].startMs)
        assertEquals(38000L, segments.intro[0].endMs)
        assertEquals(1, segments.credits.size)
        assertEquals(5800000L, segments.credits[0].startMs)
        assertNull(segments.credits[0].endMs)
    }

    @Test
    fun testParseIntroDbAppJson() {
        val json = """
            {
              "imdb_id": "tt1196946",
              "media_type": "tv",
              "is_movie": false,
              "season": 6,
              "episode": 13,
              "intro": {
                "start_sec": 155,
                "end_sec": 165,
                "start_ms": 155000,
                "end_ms": 165000,
                "confidence": 1,
                "submission_count": 1
              },
              "recap": null,
              "outro": {
                "start_sec": 2550,
                "end_sec": 2577,
                "start_ms": 2550000,
                "end_ms": 2577000,
                "confidence": 1,
                "submission_count": 1
              },
              "post_credits": null
            }
        """.trimIndent()
        val client = TheIntroDbClient()
        val segments = client.parseIntroDbAppJson(json, 6, 13)
        assertNotNull(segments)
        assertEquals(1, segments!!.intro.size)
        assertEquals(155000L, segments.intro[0].startMs)
        assertEquals(165000L, segments.intro[0].endMs)
        assertEquals(1, segments.credits.size)
        assertEquals(2550000L, segments.credits[0].startMs)
        assertEquals(2577000L, segments.credits[0].endMs)
        assertTrue(segments.recap.isEmpty())
        assertTrue(segments.preview.isEmpty())
    }
}

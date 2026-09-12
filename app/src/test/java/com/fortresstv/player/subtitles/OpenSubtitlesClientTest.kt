package com.fortresstv.player.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSubtitlesClientTest {

    @Test
    fun parseSearchResultsExtractsValidItems() {
        val json = """
            {
                "total_pages": 1,
                "total_count": 2,
                "page": 1,
                "data": [
                    {
                        "id": "sub-12345",
                        "type": "subtitle",
                        "attributes": {
                            "subtitle_id": "sub-12345",
                            "language": "pt-PT",
                            "download_count": 1420,
                            "new_download_count": 10,
                            "hearing_impaired": false,
                            "hd": true,
                            "fps": 23.976,
                            "votes": 12,
                            "ratings": 8.5,
                            "from_trusted": true,
                            "foreign_parts_only": false,
                            "upload_date": "2024-01-15T12:00:00Z",
                            "release": "Breaking.Bad.S01E01.720p.HDTV.x264",
                            "comments": "",
                            "legacy_subtitle_id": 99999,
                            "uploader": {
                                "uploader_id": 1,
                                "name": "uploaderPT",
                                "rank": "trusted"
                            },
                            "feature_details": {
                                "feature_id": 100,
                                "feature_type": "Episode",
                                "year": 2008,
                                "title": "Pilot",
                                "movie_name": "Breaking Bad",
                                "imdb_id": 903747,
                                "tmdb_id": 1396,
                                "season_number": 1,
                                "episode_number": 1,
                                "parent_imdb_id": 903747,
                                "parent_title": "Breaking Bad",
                                "parent_tmdb_id": 1396,
                                "parent_feature_id": 99
                            },
                            "url": "https://www.opensubtitles.com/en/subtitles/sub-12345",
                            "related_links": [],
                            "files": [
                                {
                                    "file_id": 88888,
                                    "cd_number": 1,
                                    "file_name": "Breaking.Bad.S01E01.Pilot.pt-PT.srt"
                                }
                            ]
                        }
                    },
                    {
                        "id": "sub-67890",
                        "type": "subtitle",
                        "attributes": {
                            "subtitle_id": "sub-67890",
                            "language": "pt-BR",
                            "download_count": 530,
                            "hearing_impaired": true,
                            "ratings": 7.0,
                            "release": "Breaking.Bad.S01E01.1080p.BluRay",
                            "files": [
                                {
                                    "file_id": 99999,
                                    "file_name": "Breaking.Bad.S01E01.pt-BR.srt"
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        val client = OpenSubtitlesClient(apiKey = "dummy_key")
        val items = client.parseSearchResults(json)

        assertEquals(2, items.size)

        val first = items[0]
        assertEquals("sub-12345", first.id)
        assertEquals(88888, first.fileId)
        assertEquals("Breaking.Bad.S01E01.Pilot.pt-PT.srt", first.fileName)
        assertEquals("pt-pt", first.language)
        assertEquals("Breaking.Bad.S01E01.720p.HDTV.x264", first.release)
        assertEquals(1420, first.downloads)
        assertEquals(8.5, first.rating, 0.001)
        assertEquals(false, first.hearingImpaired)

        val second = items[1]
        assertEquals("sub-67890", second.id)
        assertEquals(99999, second.fileId)
        assertEquals("pt-br", second.language)
        assertEquals(530, second.downloads)
        assertEquals(7.0, second.rating, 0.001)
        assertEquals(true, second.hearingImpaired)
    }

    @Test
    fun parseSearchResultsFiltersOutItemsWithoutValidFileId() {
        val json = """
            {
                "data": [
                    {
                        "id": "sub-empty-files",
                        "attributes": {
                            "language": "pt-PT",
                            "release": "Test",
                            "files": []
                        }
                    },
                    {
                        "id": "sub-negative-file-id",
                        "attributes": {
                            "language": "pt-PT",
                            "release": "Test",
                            "files": [
                                { "file_id": -1, "file_name": "invalid.srt" }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        val client = OpenSubtitlesClient(apiKey = "dummy_key")
        val items = client.parseSearchResults(json)
        assertTrue(items.isEmpty())
    }

    @Test
    fun parseSearchResultsHandlesMalformedOrEmptyData() {
        val client = OpenSubtitlesClient(apiKey = "dummy_key")
        assertTrue(client.parseSearchResults("{}").isEmpty())
        assertTrue(client.parseSearchResults("""{"data":[]}""").isEmpty())
    }
}

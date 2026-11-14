package com.fortresstv.player.xtream

import com.fortresstv.player.source.ContentSource
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files

class SeriesFirstSeenStoreTest {

    @Test
    fun firstImportEstablishesBaselineAndLaterRefreshRecordsNewSeriesOnly() {
        val dir = Files.createTempDirectory("first_seen_test").toFile()
        val store = SeriesFirstSeenStore(dir)

        // First successful import: baseline
        val initial = store.recordImport("source_1", listOf(10, 20, 30), nowEpochMs = 1_700_000_000_000L)
        assertTrue(initial.isEmpty())
        assertTrue(store.loadFirstSeen("source_1").isEmpty())

        // Second refresh: 40 is new, 10/20/30 remain baseline (no recency date)
        val second = store.recordImport("source_1", listOf(10, 20, 30, 40), nowEpochMs = 1_710_000_000_000L)
        assertEquals(mapOf(40 to 1_710_000_000_000L), second)

        // Third refresh: 40 keeps its original firstSeen, 50 gets new timestamp
        val third = store.recordImport("source_1", listOf(10, 20, 40, 50), nowEpochMs = 1_720_000_000_000L)
        assertEquals(
            mapOf(
                40 to 1_710_000_000_000L,
                50 to 1_720_000_000_000L
            ),
            third
        )

        // Independent source: source_2's first import is still its own baseline
        val source2Initial = store.recordImport("source_2", listOf(40, 99), nowEpochMs = 1_730_000_000_000L)
        assertTrue(source2Initial.isEmpty())
        assertEquals(third, store.loadFirstSeen("source_1"))
    }

    @Test
    fun parsesValidProviderAddedTimestampsAndRejectsUnusableValues() {
        // Epoch seconds
        assertEquals(1_700_000_000_000L, XtreamClient.parseProviderAddedTimestamp("1700000000"))
        // Epoch millis
        assertEquals(1_700_000_000_123L, XtreamClient.parseProviderAddedTimestamp("1700000000123"))
        // ISO-8601 instant
        assertEquals(1_704_067_200_000L, XtreamClient.parseProviderAddedTimestamp("2024-01-01T00:00:00Z"))
        // Space-separated date-time
        assertEquals(1_704_067_200_000L, XtreamClient.parseProviderAddedTimestamp("2024-01-01 00:00:00"))
        // Date only
        assertEquals(1_704_067_200_000L, XtreamClient.parseProviderAddedTimestamp("2024-01-01"))

        // Unusable values
        assertNull(XtreamClient.parseProviderAddedTimestamp(null))
        assertNull(XtreamClient.parseProviderAddedTimestamp(""))
        assertNull(XtreamClient.parseProviderAddedTimestamp("   "))
        assertNull(XtreamClient.parseProviderAddedTimestamp("0"))
        assertNull(XtreamClient.parseProviderAddedTimestamp("-1"))
        assertNull(XtreamClient.parseProviderAddedTimestamp("123"))
        assertNull(XtreamClient.parseProviderAddedTimestamp("not-a-date"))
    }

    @Test
    fun xtreamCatalogSnapshotReadsLegacyV1AndWritesV2WithAddedTimestamp() {
        val dir = Files.createTempDirectory("snapshot_compat").toFile()
        val v1File = File(dir, "series_v1.bin")
        DataOutputStream(BufferedOutputStream(v1File.outputStream())).use { out ->
            out.writeInt(0x46545843) // MAGIC
            out.writeInt(1) // VERSION 1
            out.writeByte(4) // KIND_SERIES
            out.writeInt(1) // count
            out.writeInt(77) // id
            writeTestString(out, "Legacy Show")
            out.writeBoolean(false) // categoryId
            out.writeBoolean(false) // coverUrl
            out.writeBoolean(false) // rating
            out.writeBoolean(false) // year
            out.writeBoolean(false) // plot
            out.writeBoolean(false) // ageRating
            out.writeBoolean(false) // tmdbId
            out.writeBoolean(false) // imdbId
        }

        val legacyRead = XtreamCatalogSnapshot.readSeries(v1File)
        assertNotNull(legacyRead)
        assertEquals(1, legacyRead!!.size)
        assertEquals("Legacy Show", legacyRead.single().name)
        assertNull(legacyRead.single().addedEpochMs)

        val v2File = File(dir, "series_v2.bin")
        val item = XtreamSeries(
            id = 88,
            name = "New Show",
            categoryId = "1",
            coverUrl = null,
            rating = "8.2",
            year = "2024",
            plot = null,
            ageRating = "TV-14",
            tmdbId = "1234",
            imdbId = "tt1234",
            addedEpochMs = 1_715_000_000_000L
        )
        XtreamCatalogSnapshot.writeSeries(v2File, listOf(item))
        val v2Read = XtreamCatalogSnapshot.readSeries(v2File)
        assertEquals(listOf(item), v2Read)
    }

    @Test
    fun xtreamClientRecordsBaselineOnFirstImportAndFirstSeenOnLaterRefresh() = runBlocking {
        val dir = Files.createTempDirectory("xtream_first_seen").toFile()
        var seriesPayload = """[{"series_id":1,"name":"Baseline Show"},{"series_id":2,"name":"Added Show","added":"1700000000"}]"""

        val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        server.createContext("/player_api.php") { exchange ->
            val action = exchange.requestURI.rawQuery.split('&')
                .firstOrNull { it.startsWith("action=") }?.substringAfter('=')
            val body = when (action) {
                null -> """{"user_info":{"auth":1,"status":"Active"}}"""
                "get_series" -> seriesPayload
                else -> "[]"
            }
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val client = XtreamClient(
                sourceId = "src-1",
                source = ContentSource.Xtream("http://127.0.0.1:${server.address.port}", "Test", "u", "p"),
                cacheDirectory = dir
            )
            val firstCatalog = client.loadCatalog()
            assertEquals(1_700_000_000_000L, firstCatalog.series.first { it.id == 2 }.addedEpochMs)
            assertNull(firstCatalog.series.first { it.id == 1 }.addedEpochMs)

            val store = SeriesFirstSeenStore(dir)
            assertTrue(store.loadFirstSeen("src-1").isEmpty())

            // Later refresh adds series_id=3 without an added timestamp
            seriesPayload = """[{"series_id":1,"name":"Baseline Show"},{"series_id":2,"name":"Added Show","added":"1700000000"},{"series_id":3,"name":"New Without Added"}]"""
            val secondCatalog = client.loadCatalog(forceRefresh = true)
            assertEquals(3, secondCatalog.series.size)

            val firstSeen = store.loadFirstSeen("src-1")
            assertNull(firstSeen[1])
            assertTrue((firstSeen[3] ?: 0L) > 0L)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun moviesFirstSeenStoreTracksBaselineAndPostBaselineMoviesIndependently() {
        val dir = Files.createTempDirectory("movies_first_seen_test").toFile()
        val moviesStore = MoviesFirstSeenStore(dir)
        val seriesStore = SeriesFirstSeenStore(dir)

        assertTrue(moviesStore.recordImport("src-1", listOf(10, 20), nowEpochMs = 1_700_000_000_000L).isEmpty())
        assertTrue(seriesStore.recordImport("src-1", listOf(30), nowEpochMs = 1_700_000_000_000L).isEmpty())

        val updatedMovies = moviesStore.recordImport("src-1", listOf(10, 20, 30), nowEpochMs = 1_710_000_000_000L)
        assertEquals(mapOf(30 to 1_710_000_000_000L), updatedMovies)
        assertTrue(seriesStore.loadFirstSeen("src-1").isEmpty())
    }

    private fun writeTestString(out: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size)
        out.write(bytes)
    }
}

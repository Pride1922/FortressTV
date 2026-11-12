package com.fortresstv.player.xtream

import com.fortresstv.player.source.ContentSource
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.StringReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class XtreamCatalogCacheTest {
    private val responses = mapOf(
        null to """{"user_info":{"auth":1,"status":"Active"}}""",
        "get_live_categories" to """[{"category_id":"1","category_name":"News"}]""",
        "get_live_streams" to """[{"stream_id":7,"name":"News 24","category_id":"1","epg_channel_id":"news.24","tv_archive":1,"tv_archive_duration":"3","direct_source":"https://secret.example/live"}]""",
        "get_vod_categories" to """[{"category_id":"5","category_name":"Action"}]""",
        "get_series_categories" to """[{"category_id":"9","category_name":"Drama"}]""",
        "get_vod_streams" to """[{"stream_id":100,"name":"Movie","category_id":"5","rating":7.50,"container_extension":"mkv","plot":"Ação 😀","direct_source":"https://secret.example/movie"}]""",
        "get_series" to """[{"series_id":200,"name":"Series","category_id":"9","cover":"https://img.example/s.jpg","releaseDate":"2024"}]"""
    )

    private class Provider(private val responses: Map<String?, String>) : AutoCloseable {
        val requests = AtomicInteger()
        var available = true
        private val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0).apply {
            createContext("/player_api.php") { exchange ->
                requests.incrementAndGet()
                val action = exchange.requestURI.rawQuery.split('&')
                    .firstOrNull { it.startsWith("action=") }?.substringAfter('=')
                val body = if (available) responses[action] else null
                val bytes = body?.toByteArray() ?: ByteArray(0)
                exchange.sendResponseHeaders(if (body != null) 200 else 503, if (bytes.isEmpty()) -1L else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
        val url get() = "http://127.0.0.1:${server.address.port}"
        override fun close() = server.stop(0)
    }

    private fun client(dir: File, url: String, password: String = "secret-pass") =
        XtreamClient(sourceId = "src", source = ContentSource.Xtream(url, "Provider", "user", password), cacheDirectory = dir)

    @Test
    fun cachedCatalogMatchesTheDownloadAndNeedsNoNetwork() = runBlocking {
        val dir = Files.createTempDirectory("xtream").toFile()
        Provider(responses).use { provider ->
            val downloaded = client(dir, provider.url).loadCatalog()
            assertEquals(1, downloaded.movies.size)
            assertEquals("7.5", downloaded.movies.single().rating)
            assertEquals(3, downloaded.liveChannels.single().catchupDays)

            provider.available = false
            val requestsBefore = provider.requests.get()
            val cached = client(dir, provider.url).loadCachedCatalog()

            assertNotNull(cached)
            assertTrue(cached!!.fresh)
            assertEquals(downloaded, cached.catalog)
            assertEquals("No network call when reading the cache", requestsBefore, provider.requests.get())
        }
    }

    @Test
    fun staleCacheIsStillReturnedButMarkedStale() = runBlocking {
        val dir = Files.createTempDirectory("xtream").toFile()
        Provider(responses).use { provider ->
            val downloaded = client(dir, provider.url).loadCatalog()
            val dayAndAHalfAgo = System.currentTimeMillis() - 36 * 60 * 60 * 1000L
            dir.listFiles()!!.filter { it.name.endsWith(".bin") }.forEach { it.setLastModified(dayAndAHalfAgo) }

            val cached = client(dir, provider.url).loadCachedCatalog()

            assertNotNull(cached)
            assertFalse(cached!!.fresh)
            assertEquals(downloaded, cached.catalog)
        }
    }

    @Test
    fun forcedRefreshDownloadsEvenWithAFreshCache() = runBlocking {
        val dir = Files.createTempDirectory("xtream").toFile()
        Provider(responses).use { provider ->
            client(dir, provider.url).loadCatalog()
            val requestsBefore = provider.requests.get()
            client(dir, provider.url).loadCatalog(forceRefresh = true)
            // Login + 6 list endpoints.
            assertEquals(7, provider.requests.get() - requestsBefore)
        }
    }

    @Test
    fun cacheHoldsNoCredentialsOrDirectSources() = runBlocking {
        val dir = Files.createTempDirectory("xtream").toFile()
        Provider(responses).use { provider ->
            client(dir, provider.url).loadCatalog()
        }
        val stored = dir.listFiles()!!.joinToString("") { String(it.readBytes(), Charsets.ISO_8859_1) }
        assertFalse(stored.contains("secret-pass"))
        assertFalse(stored.contains("secret.example"))
    }

    @Test
    fun changedLoginDoesNotSeeTheOldCacheAndReplacesIt() = runBlocking {
        val dir = Files.createTempDirectory("xtream").toFile()
        Provider(responses).use { provider ->
            client(dir, provider.url).loadCatalog()
            val oldSnapshots = dir.listFiles()!!.filter { it.name.endsWith(".bin") }.map { it.name }.toSet()

            val edited = client(dir, provider.url, password = "other-pass")
            assertNull(edited.loadCachedCatalog())
            edited.loadCatalog()

            val snapshots = dir.listFiles()!!.filter { it.name.endsWith(".bin") }.map { it.name }.toSet()
            assertEquals(6, snapshots.size)
            assertTrue("Snapshots of the old login are removed", snapshots.intersect(oldSnapshots).isEmpty())
        }
    }

    @Test
    fun corruptCacheReadsAsMissing() = runBlocking {
        val dir = Files.createTempDirectory("xtream").toFile()
        Provider(responses).use { provider ->
            client(dir, provider.url).loadCatalog()
        }
        val movies = dir.listFiles()!!.single { it.name.contains("get_vod_streams") }
        movies.writeBytes(movies.readBytes().copyOf(movies.length().toInt() - 3))
        assertNull(client(dir, "http://127.0.0.1:9").loadCachedCatalog())
    }

    @Test
    fun replacesLegacyJsonListCachesButKeepsDetailCaches() = runBlocking {
        val dir = Files.createTempDirectory("xtream").toFile()
        val legacyList = File(dir, "xtream_src_get_vod_streams_.json").apply { writeText("[]") }
        val legacyCategories = File(dir, "xtream_src_get_series_categories_.json").apply { writeText("[]") }
        val detail = File(dir, "xtream_src_get_series_info_series_id_5.json").apply { writeText("{}") }
        Provider(responses).use { provider ->
            client(dir, provider.url).loadCatalog()
        }
        assertFalse(legacyList.exists())
        assertFalse(legacyCategories.exists())
        assertTrue(detail.exists())
    }

    @Test
    fun streamingFieldsFollowAndroidJsonConversions() {
        val seen = mutableListOf<JsonFields>()
        XtreamJsonStream.forEachObject(
            StringReader("﻿[{\"a\":7.50,\"b\":\" 12 \",\"c\":null,\"d\":1e3,\"e\":true,\"f\":{\"x\":1},\"g\":4294967297,\"a2\":100}, 5, {\"h\":\"\\u00e9\\n\"}]")
        ) { _, fields -> seen += fields }

        assertEquals(2, seen.size)
        val first = seen[0]
        assertEquals("7.5", first.optString("a"))
        assertEquals(7, first.optInt("a", -1))
        assertEquals(12, first.optInt("b", -1))
        assertEquals("null", first.optString("c"))
        assertNull(first.nullableString("c"))
        assertEquals("1000.0", first.optString("d"))
        assertEquals("true", first.optString("e"))
        assertEquals(-1, first.optInt("e", -1))
        assertNull(first.nullableString("f"))
        assertEquals(1, first.optInt("g", -1))
        assertEquals("100", first.optString("a2"))
        assertEquals("", first.optString("missing"))
        assertEquals("é\n", seen[1].optString("h"))
    }

    @Test
    fun malformedOrNonArrayResponsesFail() {
        for (input in listOf("", "{}", "[", "[{\"a\":}]", "[\"open]")) {
            val failed = runCatching { XtreamJsonStream.forEachObject(StringReader(input)) { _, _ -> } }.isFailure
            assertTrue("Expected failure for <$input>", failed)
        }
    }
}

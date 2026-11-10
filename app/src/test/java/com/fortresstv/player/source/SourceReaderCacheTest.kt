package com.fortresstv.player.source

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.InetAddress
import java.net.InetSocketAddress

class SourceReaderCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun rejectedDownloadPreservesTheCurrentCache() = runBlocking {
        val target = folder.newFile("cached_epg.dat").apply { writeText("previous guide") }
        val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        server.createContext("/") { exchange ->
            val bytes = "<tv>".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val source = ContentSource.RemoteUrl("http://127.0.0.1:${server.address.port}/", "Test")
            val rejected = runCatching {
                SourceReader.replaceRemoteCache(source, target) { staged, _ ->
                    assertEquals("<tv>", staged.readText())
                    throw IllegalArgumentException("Invalid guide")
                }
            }
            assertTrue(rejected.exceptionOrNull() is IllegalArgumentException)
            assertEquals("previous guide", target.readText())
            assertEquals(listOf(target.name), folder.root.list()?.toList())
        } finally {
            server.stop(0)
        }
    }
}

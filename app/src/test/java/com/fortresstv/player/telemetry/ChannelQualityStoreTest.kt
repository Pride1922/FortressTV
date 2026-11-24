package com.fortresstv.player.telemetry

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class ChannelQualityStoreTest {
    private fun variant(source: String) = playbackStreamVariantId(source, "channel", "https://example.com/stream")!!

    @Test
    fun keepsNewestMeasurementForEachSourceWithoutInferringMissingQuality() {
        val first = variant("first")
        val second = variant("second")
        val existing = mapOf(first to MeasuredVideoQuality(3840, 2160, 2_000))
        val merged = mergeChannelQualities(existing, mapOf(first to MeasuredVideoQuality(1280, 720, 1_000),
            second to MeasuredVideoQuality(1920, 1080, 3_000)))
        assertEquals("4K", merged[first]!!.tier)
        assertEquals("FHD", merged[second]!!.tier)
        assertEquals(merged, mergeChannelQualities(merged, emptyMap()))
        assertNull(merged[variant("unknown")])
    }

    @Test
    fun laterLowerResolutionReplacesOlder4kAndCacheIsBounded() {
        val id = variant("source")
        val merged = mergeChannelQualities(mapOf(id to MeasuredVideoQuality(3840, 2160, 1_000)),
            mapOf(id to MeasuredVideoQuality(1920, 1080, 2_000)))
        assertEquals("FHD", merged[id]!!.tier)
        val many = (1..550).associate { variant("source-$it") to MeasuredVideoQuality(1920, 1080, it.toLong()) }
        val bounded = mergeChannelQualities(emptyMap(), many)
        assertEquals(500, bounded.size)
        assertFalse(bounded.containsKey(variant("source-1")))
        assertTrue(bounded.containsKey(variant("source-550")))
    }

    @Test
    fun sharedPreferencesCacheSurvivesRecreationAndDoesNotMixNamespaces() {
        val storage = mutableMapOf<String, String>()
        val prefs = fakePreferences(storage)
        val first = ChannelQualityStore { prefs }
        val id = variant("source")
        val quality = MeasuredVideoQuality(3840, 2160, 1_000)
        first.mergeObservations(mapOf(id to quality))
        val recreated = ChannelQualityStore { prefs }
        recreated.mergeObservations(emptyMap())
        assertEquals(quality, recreated.snapshots.value[id])
        val other = ChannelQualityStore { fakePreferences(mutableMapOf()) }
        other.mergeObservations(emptyMap())
        assertTrue(other.snapshots.value.isEmpty())
        assertFalse(storage.values.single().contains("https://"))
    }

    private fun fakePreferences(values: MutableMap<String, String>): SharedPreferences {
        lateinit var editor: SharedPreferences.Editor
        editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { _, method, args ->
            when (method.name) {
                "putString" -> { values[args[0] as String] = args[1] as String; editor }
                "apply" -> null
                else -> error("Unexpected editor call: ${method.name}")
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString" -> values[args[0] as String] ?: args[1]
                "edit" -> editor
                else -> error("Unexpected preferences call: ${method.name}")
            }
        } as SharedPreferences
    }
}

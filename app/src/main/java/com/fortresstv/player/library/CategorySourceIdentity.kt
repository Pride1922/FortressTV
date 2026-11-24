package com.fortresstv.player.library

internal fun categoryCloudSourceId(sourceId: String): String =
    runCatching { java.util.UUID.fromString(sourceId).toString() }.getOrNull()
        ?: java.util.UUID.nameUUIDFromBytes("source_$sourceId".toByteArray(Charsets.UTF_8)).toString()

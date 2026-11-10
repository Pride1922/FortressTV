package com.fortresstv.player

import java.net.URI

internal val youTubeVideoIdPattern = Regex("[A-Za-z0-9_-]{11}")

internal fun extractYouTubeVideoId(urlOrKey: String?): String? {
    val value = urlOrKey?.trim().orEmpty()
    if (youTubeVideoIdPattern.matches(value)) return value

    val uri = trailerWebUri(value) ?: return null
    val path = uri.path.orEmpty().trimEnd('/')
    val candidate = when (uri.host.lowercase()) {
        "youtu.be" -> path.removePrefix("/")
        "youtube.com", "www.youtube.com", "m.youtube.com" -> when {
            path == "/watch" -> uri.rawQuery?.split('&')
                ?.firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
            path.startsWith("/embed/") -> path.removePrefix("/embed/")
            path.startsWith("/shorts/") -> path.removePrefix("/shorts/")
            path.startsWith("/live/") -> path.removePrefix("/live/")
            else -> null
        }
        "youtube-nocookie.com", "www.youtube-nocookie.com" -> path.removePrefix("/embed/")
            .takeIf { path.startsWith("/embed/") }
        else -> null
    }
    return candidate?.takeIf(youTubeVideoIdPattern::matches)
}

internal fun trailerWebUri(value: String): URI? =
    runCatching { URI(value.trim()) }.getOrNull()?.takeIf {
        it.host != null && it.userInfo == null && it.scheme?.lowercase() in setOf("https", "http")
    }

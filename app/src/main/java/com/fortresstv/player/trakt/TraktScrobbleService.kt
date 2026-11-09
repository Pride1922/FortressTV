package com.fortresstv.player.trakt

import android.content.Context
import com.fortresstv.player.FortressLibrary
import com.fortresstv.player.VodPlaybackRequest
import com.fortresstv.player.library.VodProgressKind
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

internal enum class ScrobbleAction {
    START,
    PAUSE,
    STOP
}

internal open class TraktScrobbleService(
    val context: Context,
    val profileNamespace: String,
    val traktStore: TraktSettingsStore = TraktSettingsStore(context, profileNamespace),
    val client: TraktClient = TraktClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    val debounceDelayMs: Long = 800L
) {
    private val stateMutex = Mutex()
    private var lastAction: ScrobbleAction? = null
    private var lastReportedPositionMs: Long = -1L
    private var lastReportedActionTimeMs: Long = 0L
    private var pendingPauseJob: Job? = null

    /**
     * Release 1 rule: Only scrobble items with verified external IDs.
     * Skip ambiguous title-only matches without guessing.
     */
    fun canScrobble(request: VodPlaybackRequest): Boolean {
        if (!traktStore.isConnected() || !traktStore.isScrobbleEnabled()) {
            return false
        }
        val hasVerifiedId = !request.tmdbId.isNullOrBlank() || !request.imdbId.isNullOrBlank()
        return when (request.kind) {
            VodProgressKind.MOVIE -> hasVerifiedId
            VodProgressKind.EPISODE -> hasVerifiedId && request.season != null && request.episode != null
            VodProgressKind.CATCHUP -> false
        }
    }

    /**
     * Emitted when video playback starts or resumes from pause.
     */
    fun onPlaybackStarted(positionMs: Long, durationMs: Long, request: VodPlaybackRequest) {
        if (!canScrobble(request)) return

        scope.launch {
            val shouldDispatch = stateMutex.withLock {
                // Cancel any pending debounced pause event
                pendingPauseJob?.cancel()
                pendingPauseJob = null

                val now = System.currentTimeMillis()
                // Deduplicate start if already in START state and position hasn't jumped
                if (lastAction == ScrobbleAction.START &&
                    kotlin.math.abs(positionMs - lastReportedPositionMs) < 5_000L &&
                    (now - lastReportedActionTimeMs) < 15_000L
                ) {
                    return@withLock false
                }

                lastAction = ScrobbleAction.START
                lastReportedPositionMs = positionMs
                lastReportedActionTimeMs = now
                true
            }

            if (shouldDispatch) {
                dispatchScrobble(ScrobbleAction.START, positionMs, durationMs, request)
            }
        }
    }

    /**
     * Emitted when video playback is paused.
     * Buffering events are ignored so stalls do not generate spurious scrobbles.
     */
    fun onPlaybackPaused(
        positionMs: Long,
        durationMs: Long,
        request: VodPlaybackRequest,
        isBuffering: Boolean = false
    ) {
        if (isBuffering || !canScrobble(request)) return

        scope.launch {
            stateMutex.withLock {
                if (lastAction == ScrobbleAction.PAUSE || lastAction == ScrobbleAction.STOP) {
                    return@withLock
                }

                // Debounce rapid pause-play toggles (e.g. seeking or audio focus transient switches)
                pendingPauseJob?.cancel()
                pendingPauseJob = scope.launch {
                    delay(debounceDelayMs)
                    stateMutex.withLock {
                        lastAction = ScrobbleAction.PAUSE
                        lastReportedPositionMs = positionMs
                        lastReportedActionTimeMs = System.currentTimeMillis()
                    }
                    dispatchScrobble(ScrobbleAction.PAUSE, positionMs, durationMs, request)
                }
            }
        }
    }

    /**
     * Emitted when video playback stops, finishes, or the player dialog is dismissed.
     */
    fun onPlaybackStopped(positionMs: Long, durationMs: Long, request: VodPlaybackRequest) {
        if (!canScrobble(request)) return

        scope.launch(NonCancellable) {
            stateMutex.withLock {
                pendingPauseJob?.cancel()
                pendingPauseJob = null

                if (lastAction == ScrobbleAction.STOP) {
                    return@withLock
                }
                lastAction = ScrobbleAction.STOP
                lastReportedPositionMs = positionMs
                lastReportedActionTimeMs = System.currentTimeMillis()
            }

            dispatchScrobble(ScrobbleAction.STOP, positionMs, durationMs, request)
        }
    }

    private suspend fun dispatchScrobble(
        action: ScrobbleAction,
        positionMs: Long,
        durationMs: Long,
        request: VodPlaybackRequest
    ) {
        val clientId = traktStore.loadClientId()
        if (clientId.isBlank()) return

        val accessToken = traktStore.getValidAccessToken(clientId, client) ?: return
        val progress = calculateProgressPercent(positionMs, durationMs)
        val payload = buildScrobblePayload(request, progress)

        val actionName = when (action) {
            ScrobbleAction.START -> "start"
            ScrobbleAction.PAUSE -> "pause"
            ScrobbleAction.STOP -> "stop"
        }

        client.scrobble(actionName, payload, accessToken, clientId)
    }

    companion object {
        fun calculateProgressPercent(positionMs: Long, durationMs: Long): Float {
            if (durationMs <= 0L) return 0f
            val percent = (positionMs.toDouble() / durationMs.toDouble()) * 100.0
            return percent.toFloat().coerceIn(0f, 100f)
        }

        fun buildScrobblePayload(request: VodPlaybackRequest, progressPercent: Float): JSONObject {
            val root = JSONObject()
            root.put("progress", kotlin.math.round(progressPercent * 10.0) / 10.0)

            val ids = JSONObject()
            request.tmdbId?.toIntOrNull()?.let { ids.put("tmdb", it) }
            request.imdbId?.takeIf { it.isNotBlank() }?.let { ids.put("imdb", it) }

            when (request.kind) {
                VodProgressKind.MOVIE -> {
                    val movie = JSONObject()
                    movie.put("title", request.title)
                    movie.put("ids", ids)
                    root.put("movie", movie)
                }
                VodProgressKind.EPISODE -> {
                    val show = JSONObject()
                    show.put("title", request.seriesName ?: request.title)
                    show.put("ids", ids)
                    root.put("show", show)

                    val episode = JSONObject()
                    request.season?.let { episode.put("season", it) }
                    request.episode?.let { episode.put("number", it) }
                    root.put("episode", episode)
                }
                VodProgressKind.CATCHUP -> {}
            }

            return root
        }
    }
}

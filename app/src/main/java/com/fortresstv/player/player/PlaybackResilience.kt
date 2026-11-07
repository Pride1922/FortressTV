package com.fortresstv.player.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Network
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.ParserException
import androidx.media3.common.VideoSize
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import java.io.IOException
import kotlin.math.min

enum class PlaybackMode(val persistedValue: String) {
    AUTO("auto"),
    STABLE("stable"),
    DATA_SAVER("data_saver");

    companion object {
        fun fromPersisted(value: String?): PlaybackMode = entries.firstOrNull {
            it.persistedValue == value
        } ?: AUTO
    }
}

val LocalPlaybackMode = staticCompositionLocalOf { PlaybackMode.AUTO }

enum class PlaybackContentType { LIVE, VOD }

data class PlaybackBufferProfile(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val playbackBufferMs: Int,
    val rebufferMs: Int,
    val targetLiveOffsetMs: Long? = null,
    val maxVideoBitrate: Int? = null
)

class PlaybackTransferProgress {
    @Volatile
    var bytesObserved: Long = 0
        private set

    fun record(bytes: Long) {
        if (bytes > 0) bytesObserved += bytes
    }
}

class PlaybackFrameProgress(initialRealtimeMs: Long) {
    @Volatile
    var lastFrameRealtimeMs: Long = initialRealtimeMs
        private set

    fun recordFrame(realtimeMs: Long) {
        lastFrameRealtimeMs = realtimeMs
    }
}

object ReadyFreezePolicy {
    fun shouldRecover(
        isPlaying: Boolean,
        hasVideo: Boolean,
        networkAvailable: Boolean,
        millisSinceLastFrame: Long,
        freezeThresholdMs: Long = 12_000L
    ): Boolean = isPlaying && hasVideo && networkAvailable && millisSinceLastFrame >= freezeThresholdMs

    fun shouldShowNotice(
        isPlaying: Boolean,
        hasVideo: Boolean,
        millisSinceLastFrame: Long,
        noticeThresholdMs: Long = 3_500L
    ): Boolean = isPlaying && hasVideo && millisSinceLastFrame >= noticeThresholdMs
}

object PlaybackProfiles {
    fun forContent(type: PlaybackContentType, mode: PlaybackMode): PlaybackBufferProfile = when (type) {
        PlaybackContentType.LIVE -> when (mode) {
            PlaybackMode.AUTO -> PlaybackBufferProfile(12_000, 35_000, 2_000, 4_000, 12_000)
            PlaybackMode.STABLE -> PlaybackBufferProfile(25_000, 60_000, 4_000, 8_000, 25_000)
            PlaybackMode.DATA_SAVER -> PlaybackBufferProfile(15_000, 40_000, 2_500, 5_000, 15_000, 2_000_000)
        }
        PlaybackContentType.VOD -> when (mode) {
            PlaybackMode.AUTO -> PlaybackBufferProfile(25_000, 60_000, 2_000, 4_000)
            PlaybackMode.STABLE -> PlaybackBufferProfile(40_000, 90_000, 4_000, 8_000)
            PlaybackMode.DATA_SAVER -> PlaybackBufferProfile(30_000, 75_000, 2_500, 5_000, maxVideoBitrate = 2_000_000)
        }
    }
}

fun Context.isPlaybackNetworkAvailable(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    // Validation probes can fail on an otherwise usable carrier/VPN/private route.
    // Let the actual stream request determine reachability; no default route means wait.
    return manager.activeNetwork != null
}

@Composable
fun rememberPlaybackNetworkGeneration(): Int {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var generation by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mainHandler = Handler(Looper.getMainLooper())
        val initialNetwork = manager.activeNetwork
        val initialCapabilities = initialNetwork?.let(manager::getNetworkCapabilities)
        val tracker = PlaybackNetworkTracker(
            initialNetwork,
            initialCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            initialCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        )
        var disposed = false
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (tracker.update(
                    network,
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )) {
                    mainHandler.post { if (!disposed) generation++ }
                }
            }

            override fun onLost(network: Network) { tracker.lost(network) }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        onDispose {
            disposed = true
            runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }
    return generation
}

@UnstableApi
private class PlaybackDiagnosticsListener(
    private val type: PlaybackContentType,
    private val mode: PlaybackMode,
    private val transferProgress: PlaybackTransferProgress?
) : AnalyticsListener {
    private val startedAtMs = android.os.SystemClock.elapsedRealtime()
    private var rebufferCount = 0
    private var lastBandwidthLogMs = 0L

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        if (state == androidx.media3.common.Player.STATE_BUFFERING && eventTime.currentPlaybackPositionMs > 0) {
            rebufferCount++
        }
        if (state == androidx.media3.common.Player.STATE_READY) {
            Log.d(
                "FortressTV.QoE",
                "type=$type mode=$mode readyMs=${android.os.SystemClock.elapsedRealtime() - startedAtMs} rebuffers=$rebufferCount"
            )
        }
    }

    override fun onBandwidthEstimate(
        eventTime: AnalyticsListener.EventTime,
        totalLoadTimeMs: Int,
        totalBytesLoaded: Long,
        bitrateEstimate: Long
    ) {
        transferProgress?.record(totalBytesLoaded)
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastBandwidthLogMs >= 10_000L) {
            lastBandwidthLogMs = now
            Log.d("FortressTV.QoE", "type=$type bandwidthKbps=${bitrateEstimate / 1000}")
        }
    }

    override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, videoSize: VideoSize) {
        Log.d("FortressTV.QoE", "type=$type video=${videoSize.width}x${videoSize.height}")
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long
    ) {
        Log.d("FortressTV.QoE", "type=$type droppedVideoFrames=$droppedFrames elapsedMs=$elapsedMs")
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: androidx.media3.common.Format,
        decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
    ) {
        // Only technical format fields: never log Format itself or source identifiers.
        Log.d(
            "FortressTV.QoE",
            "type=$type videoInput=${format.width}x${format.height} fps=${format.frameRate} mime=${format.sampleMimeType}"
        )
    }

    override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
        val videoGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        val adaptive = videoGroups.any { it.length > 1 }
        val selected = videoGroups.firstNotNullOfOrNull { group ->
            (0 until group.length).firstOrNull(group::isTrackSelected)?.let(group::getTrackFormat)
        }
        Log.d(
            "FortressTV.QoE",
            "type=$type adaptive=$adaptive selectedKbps=${selected?.bitrate?.takeIf { it > 0 }?.div(1000)} resolution=${selected?.width}x${selected?.height}"
        )
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        Log.w("FortressTV.QoE", "type=$type errorCode=${error.errorCodeName} rebuffers=$rebufferCount")
    }
}

enum class StallDecision { WAIT, RECOVER, GIVE_UP }

object StallRecoveryPolicy {
    fun decide(
        positionProgressMs: Long,
        bufferProgressMs: Long,
        recoveryAttempts: Int,
        networkAvailable: Boolean,
        bytesProgress: Long = 0,
        minimumProgressMs: Long = 500,
        maximumAttempts: Int = 3
    ): StallDecision {
        if (!networkAvailable) return StallDecision.WAIT
        if (positionProgressMs >= minimumProgressMs || bufferProgressMs >= minimumProgressMs || bytesProgress > 0) {
            return StallDecision.WAIT
        }
        return if (recoveryAttempts >= maximumAttempts) StallDecision.GIVE_UP else StallDecision.RECOVER
    }
}

object LiveEndedRecoveryPolicy {
    fun decide(
        playbackState: Int,
        playWhenReady: Boolean,
        networkAvailable: Boolean,
        recoveryAttempts: Int,
        maximumAttempts: Int = 3
    ): StallDecision {
        if (playbackState != Player.STATE_ENDED || !playWhenReady) return StallDecision.WAIT
        if (!networkAvailable) return StallDecision.WAIT
        return if (recoveryAttempts >= maximumAttempts) StallDecision.GIVE_UP else StallDecision.RECOVER
    }
}

@UnstableApi
object PlaybackRetryRules {
    fun retryDelayMs(error: IOException, errorCount: Int): Long? {
        if (error is ParserException) return null
        val responseCode = (error as? HttpDataSource.InvalidResponseCodeException)?.responseCode
        return retryDelayMs(responseCode, errorCount)
    }

    fun retryDelayMs(responseCode: Int?, errorCount: Int): Long? {
        if (responseCode in listOf(401, 403, 404)) return null
        val transientHttp = responseCode == 408 || responseCode == 429 || responseCode in 500..599
        if (responseCode != null && !transientHttp) return null
        if (errorCount > 5) return null
        val exponent = (errorCount - 1).coerceIn(0, 4)
        return min(16_000L, 1_000L shl exponent)
    }
}

@UnstableApi
class FortressLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy(5) {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        return PlaybackRetryRules.retryDelayMs(
            loadErrorInfo.exception,
            loadErrorInfo.errorCount
        ) ?: C.TIME_UNSET
    }
}

@UnstableApi
object FortressPlayerFactory {
    fun create(
        context: Context,
        type: PlaybackContentType,
        mode: PlaybackMode,
        transferProgress: PlaybackTransferProgress? = null,
        frameProgress: PlaybackFrameProgress? = null
    ): ExoPlayer {
        val profile = PlaybackProfiles.forContent(type, mode)
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val capabilities = manager?.activeNetwork?.let(manager::getNetworkCapabilities)
        val networkType = when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "cellular"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "wifi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ethernet"
            else -> "other"
        }
        Log.d("FortressTV.QoE", "create type=$type mode=$mode network=$networkType metered=${manager?.isActiveNetworkMetered}")
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                profile.minBufferMs,
                profile.maxBufferMs,
                profile.playbackBufferMs,
                profile.rebufferMs
            )
            .build()
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("FortressTV")
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(FortressLoadErrorHandlingPolicy())
        val trackSelector = DefaultTrackSelector(context).apply {
            if (profile.maxVideoBitrate != null) {
                parameters = buildUponParameters()
                    .setMaxVideoBitrate(profile.maxVideoBitrate)
                    .build()
            }
        }
        val miBoxExperiment = type == PlaybackContentType.LIVE &&
            android.os.Build.MODEL == "MIBOX4" && android.os.Build.DEVICE == "oneday" &&
            android.os.Build.VERSION.SDK_INT == 28
        val renderersFactory = (if (miBoxExperiment) MiBoxRenderersFactory(context)
            else DefaultRenderersFactory(context))
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        if (miBoxExperiment) Log.d("FortressTV.QoE", "MiBox identical-release skip experiment enabled")
        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                addAnalyticsListener(PlaybackDiagnosticsListener(type, mode, transferProgress))
                if (frameProgress != null) {
                    var sampleStartMs = 0L
                    var sampleStartPresentationUs = 0L
                    var previousPresentationUs = 0L
                    var smallestFrameStepUs = Long.MAX_VALUE
                    var sampledFrames = 0
                    setVideoFrameMetadataListener { presentationUs, _, _, _ ->
                        val now = android.os.SystemClock.elapsedRealtime()
                        frameProgress.recordFrame(now)
                        if (sampledFrames == 0 || presentationUs <= previousPresentationUs) {
                            sampleStartMs = now
                            sampleStartPresentationUs = presentationUs
                            smallestFrameStepUs = Long.MAX_VALUE
                            sampledFrames = 0
                        } else {
                            smallestFrameStepUs = minOf(smallestFrameStepUs, presentationUs - previousPresentationUs)
                        }
                        previousPresentationUs = presentationUs
                        sampledFrames++
                        if (now - sampleStartMs >= 10_000L && sampledFrames > 1) {
                            val mediaSpanUs = presentationUs - sampleStartPresentationUs
                            Log.d(
                                "FortressTV.QoE",
                                "type=$type submittedFps=${(sampledFrames - 1) * 1000.0 / (now - sampleStartMs)} " +
                                    "timestampFps=${(sampledFrames - 1) * 1_000_000.0 / mediaSpanUs} minFrameStepUs=$smallestFrameStepUs"
                            )
                            sampledFrames = 0
                        }
                    }
                }
            }
    }

    fun mediaItem(url: String, type: PlaybackContentType, mode: PlaybackMode): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        if (type == PlaybackContentType.LIVE) {
            val target = PlaybackProfiles.forContent(type, mode).targetLiveOffsetMs
            if (target != null) {
                builder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(target)
                        .setMinPlaybackSpeed(0.97f)
                        .setMaxPlaybackSpeed(1.03f)
                        .build()
                )
            }
        }
        return builder.build()
    }
}

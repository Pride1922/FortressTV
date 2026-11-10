package com.fortresstv.player.player

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.analytics.AnalyticsListener
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLException

private const val TAG = "FortressTV.Tune"

/** Each benchmark media source owns its transfer listener and opaque analytics tag. */
@UnstableApi
internal class LiveTuneBenchmark(
    private val selectedAtMs: Long,
    mode: PlaybackMode,
    loopback: Boolean
) : TransferListener {
    private val id = nextId.incrementAndGet()
    private val nextAttempt = AtomicInteger()
    private val firstBytesAtMs = AtomicLong()
    @Volatile private var firstFrameAtMs = 0L
    private var bufferingAtMs = 0L

    init {
        Log.d(TAG, "tune=$id start mode=$mode loopback=$loopback prepareMs=${SystemClock.elapsedRealtime() - selectedAtMs}")
    }

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    fun openHttp(source: HttpDataSource, dataSpec: DataSpec): Long {
        if (firstFrameAtMs != 0L) return source.open(dataSpec)

        val attempt = nextAttempt.incrementAndGet()
        val startedAt = SystemClock.elapsedRealtime()
        Log.d(TAG, "tune=$id attempt=$attempt openStartMs=${startedAt - selectedAtMs}")
        try {
            val length = source.open(dataSpec)
            val duration = SystemClock.elapsedRealtime() - startedAt
            Log.d(TAG, "tune=$id attempt=$attempt openMs=$duration status=${source.responseCode}")
            return length
        } catch (error: IOException) {
            val duration = SystemClock.elapsedRealtime() - startedAt
            val status = (error as? HttpDataSource.InvalidResponseCodeException)?.responseCode ?: source.responseCode
            Log.d(TAG, "tune=$id attempt=$attempt openMs=$duration status=$status failure=${failureKind(error)}")
            throw error
        }
    }

    fun recordRetryDecision(error: IOException, errorCount: Int, delayMs: Long) {
        if (firstFrameAtMs != 0L) return
        val requestError = if (error is LiveStartupTimeoutException) error.cause else error
        val stage = when ((requestError as? HttpDataSource.HttpDataSourceException)?.type) {
            HttpDataSource.HttpDataSourceException.TYPE_OPEN -> "open"
            HttpDataSource.HttpDataSourceException.TYPE_READ -> "read"
            HttpDataSource.HttpDataSourceException.TYPE_CLOSE -> "close"
            else -> "load"
        }
        val decisionAt = SystemClock.elapsedRealtime() - selectedAtMs
        val delay = if (delayMs == C.TIME_UNSET) -1L else delayMs
        Log.d(TAG, "tune=$id retryAtMs=$decisionAt errorCount=$errorCount stage=$stage delayMs=$delay failure=${failureKind(error)}")
    }

    private fun failureKind(error: IOException): String {
        if (error is HttpDataSource.InvalidResponseCodeException) return "http_status"
        return generateSequence<Throwable>(error) { it.cause }.firstNotNullOfOrNull { cause ->
            when (cause) {
                is UnknownHostException -> "dns"
                is ConnectException -> "connect"
                is SSLException -> "tls"
                is SocketTimeoutException -> "timeout"
                else -> null
            }
        } ?: "io"
    }

    override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
        if (!isNetwork || bytesTransferred <= 0 || firstBytesAtMs.get() != 0L) return
        val now = SystemClock.elapsedRealtime()
        if (firstBytesAtMs.compareAndSet(0L, now)) {
            Log.d(TAG, "tune=$id firstBytesMs=${now - selectedAtMs}")
        }
    }

    fun recordFirstFrame(renderTimeMs: Long) {
        if (firstFrameAtMs != 0L) return
        firstFrameAtMs = renderTimeMs
        val firstBytes = firstBytesAtMs.get()
        val bytesToFrameMs = if (firstBytes > 0L) renderTimeMs - firstBytes else -1L
        Log.d(TAG, "tune=$id firstFrameMs=${renderTimeMs - selectedAtMs} bytesToFrameMs=$bytesToFrameMs")
    }

    fun recordPlaybackState(state: Int, realtimeMs: Long) {
        if (firstFrameAtMs == 0L) return
        if (state == Player.STATE_BUFFERING && realtimeMs - firstFrameAtMs <= 30_000L) {
            bufferingAtMs = realtimeMs
        } else if (state == Player.STATE_READY && bufferingAtMs != 0L) {
            Log.d(TAG, "tune=$id stallMs=${realtimeMs - bufferingAtMs}")
            bufferingAtMs = 0L
        }
    }

    fun recordError(error: PlaybackException) {
        Log.d(TAG, "tune=$id error=${error.errorCodeName}")
    }

    private companion object {
        val nextId = AtomicInteger()
    }
}

@UnstableApi
internal class LiveTuneBenchmarkListener : AnalyticsListener {
    private val window = Timeline.Window()

    override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
        tune(eventTime)?.recordFirstFrame(renderTimeMs)
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        tune(eventTime)?.recordPlaybackState(state, eventTime.realtimeMs)
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        tune(eventTime)?.recordError(error)
    }

    private fun tune(eventTime: AnalyticsListener.EventTime): LiveTuneBenchmark? {
        if (eventTime.windowIndex !in 0 until eventTime.timeline.windowCount) return null
        return eventTime.timeline.getWindow(eventTime.windowIndex, window).mediaItem.localConfiguration?.tag as? LiveTuneBenchmark
    }
}

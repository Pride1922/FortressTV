package com.fortresstv.player.player

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener

/** Device-scoped experiment: retain outputs sharing a scheduled release time. */
@UnstableApi
internal class MiBoxRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>
    ) {
        val firstVideoIndex = out.size
        super.buildVideoRenderers(
            context, extensionRendererMode, mediaCodecSelector, enableDecoderFallback,
            eventHandler, eventListener, allowedVideoJoiningTimeMs, out
        )
        // Keep the normal extension renderers and all audio/text construction intact.
        val index = (firstVideoIndex until out.size).firstOrNull {
            out[it].javaClass == MediaCodecVideoRenderer::class.java
        } ?: return
        val builder = MediaCodecVideoRenderer.Builder(context)
            .setCodecAdapterFactory(codecAdapterFactory)
            .setMediaCodecSelector(mediaCodecSelector)
            .setAllowedJoiningTimeMs(allowedVideoJoiningTimeMs)
            .setEnableDecoderFallback(enableDecoderFallback)
            .setEventHandler(eventHandler)
            .setEventListener(eventListener)
            .setMaxDroppedFramesToNotify(50)
        out[index] = object : MediaCodecVideoRenderer(builder) {
            override fun shouldSkipBuffersWithIdenticalReleaseTime(): Boolean =
                if (codecInfo?.name?.startsWith("OMX.amlogic.") == true) false
                else super.shouldSkipBuffersWithIdenticalReleaseTime()
        }
    }
}

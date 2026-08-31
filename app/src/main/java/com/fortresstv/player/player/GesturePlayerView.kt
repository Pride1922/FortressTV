package com.fortresstv.player.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewParent
import android.view.Window
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlin.math.abs
import kotlin.math.roundToInt

enum class GestureControl { BRIGHTNESS, VOLUME }

data class GestureLevel(val control: GestureControl, val value: Float)

@UnstableApi
class GesturePlayerView(context: Context) : PlayerView(context) {
    var onGestureLevelChanged: ((GestureLevel) -> Unit)? = null
    var onSingleTap: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var downX = 0f
    private var downY = 0f
    private var initialLevel = 0f
    private var activeControl = GestureControl.BRIGHTNESS
    private var adjusting = false
    private var originalWindowBrightness: Float? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                adjusting = false
                activeControl = if (event.x < width / 2f) GestureControl.BRIGHTNESS else GestureControl.VOLUME
                initialLevel = when (activeControl) {
                    GestureControl.BRIGHTNESS -> currentBrightness()
                    GestureControl.VOLUME -> currentVolume()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val verticalDistance = downY - event.y
                val horizontalDistance = event.x - downX
                if (!adjusting && abs(verticalDistance) > touchSlop && abs(verticalDistance) > abs(horizontalDistance)) {
                    adjusting = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (adjusting && height > 0) {
                    val gestureRange = (height * 0.75f).coerceAtLeast(1f)
                    val level = (initialLevel + verticalDistance / gestureRange).coerceIn(0f, 1f)
                    when (activeControl) {
                        GestureControl.BRIGHTNESS -> setBrightness(level)
                        GestureControl.VOLUME -> setVolume(level)
                    }
                    onGestureLevelChanged?.invoke(GestureLevel(activeControl, level))
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (!adjusting) {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)
                    if (dx <= touchSlop && dy <= touchSlop) {
                        onSingleTap?.invoke()
                        performClick()
                    }
                }
                val wasAdjusting = adjusting
                adjusting = false
                if (wasAdjusting) return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                adjusting = false
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun currentBrightness(): Float {
        val windowLevel = currentWindow()?.attributes?.screenBrightness ?: -1f
        if (windowLevel >= 0f) return windowLevel
        val systemLevel = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(128)
        return systemLevel / 255f
    }

    private fun setBrightness(level: Float) {
        val window = currentWindow() ?: return
        if (originalWindowBrightness == null) originalWindowBrightness = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = level.coerceAtLeast(.01f) }
    }

    private fun currentVolume(): Float {
        val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maximum.toFloat()
    }

    private fun setVolume(level: Float) {
        val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (level * maximum).roundToInt(), 0)
    }

    override fun onDetachedFromWindow() {
        originalWindowBrightness?.let { original ->
            currentWindow()?.let { window ->
                window.attributes = window.attributes.apply { screenBrightness = original }
            }
        }
        super.onDetachedFromWindow()
    }

    private fun currentWindow(): Window? {
        var current: ViewParent? = parent
        while (current != null) {
            if (current is DialogWindowProvider) return current.window
            current = current.parent
        }
        return context.findActivity()?.window
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

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

private enum class DragAction {
    NONE,
    BRIGHTNESS,
    VOLUME,
    CENTER_VERTICAL,
    EDGE_SWIPE_RIGHT,
    HORIZONTAL_SEEK
}

@UnstableApi
class GesturePlayerView(context: Context) : PlayerView(context) {
    var onGestureLevelChanged: ((GestureLevel) -> Unit)? = null
    var onSingleTap: (() -> Unit)? = null
    var onCenterSwipeUp: (() -> Unit)? = null
    var onCenterSwipeDown: (() -> Unit)? = null
    var onEdgeSwipeRight: (() -> Unit)? = null
    var onHorizontalSeek: ((distanceX: Float, finished: Boolean) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var downX = 0f
    private var downY = 0f
    private var initialLevel = 0f
    private var dragAction = DragAction.NONE
    private var swipeTriggered = false
    private var originalWindowBrightness: Float? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                dragAction = DragAction.NONE
                swipeTriggered = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = downY - event.y // Positive = moving upwards, Negative = moving downwards

                if (dragAction == DragAction.NONE) {
                    val absDy = abs(dy)
                    val absDx = abs(dx)
                    if (absDy > touchSlop && absDy > absDx) {
                        val screenWidth = width.toFloat().coerceAtLeast(1f)
                        val fractionX = downX / screenWidth
                        when {
                            fractionX < 0.32f -> {
                                dragAction = DragAction.BRIGHTNESS
                                initialLevel = currentBrightness()
                                parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            fractionX > 0.68f -> {
                                dragAction = DragAction.VOLUME
                                initialLevel = currentVolume()
                                parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            else -> {
                                dragAction = DragAction.CENTER_VERTICAL
                                parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                    } else if (absDx > touchSlop && absDx > absDy) {
                        val screenWidth = width.toFloat().coerceAtLeast(1f)
                        if (downX < screenWidth * 0.15f && dx > 0) {
                            dragAction = DragAction.EDGE_SWIPE_RIGHT
                            parent?.requestDisallowInterceptTouchEvent(true)
                        } else if (onHorizontalSeek != null) {
                            dragAction = DragAction.HORIZONTAL_SEEK
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }

                if (height > 0) {
                    val gestureRange = (height * 0.45f).coerceAtLeast(300f)
                    when (dragAction) {
                        DragAction.BRIGHTNESS -> {
                            val level = (initialLevel + dy / gestureRange).coerceIn(0.01f, 1f)
                            setBrightness(level)
                            onGestureLevelChanged?.invoke(GestureLevel(GestureControl.BRIGHTNESS, level))
                            return true
                        }
                        DragAction.VOLUME -> {
                            val level = (initialLevel + dy / gestureRange).coerceIn(0f, 1f)
                            setVolume(level)
                            onGestureLevelChanged?.invoke(GestureLevel(GestureControl.VOLUME, level))
                            return true
                        }
                        DragAction.CENTER_VERTICAL -> {
                            if (!swipeTriggered) {
                                if (dy > touchSlop * 2) {
                                    swipeTriggered = true
                                    onCenterSwipeUp?.invoke()
                                } else if (dy < -touchSlop * 2) {
                                    swipeTriggered = true
                                    onCenterSwipeDown?.invoke()
                                }
                            }
                            return true
                        }
                        DragAction.EDGE_SWIPE_RIGHT -> {
                            if (!swipeTriggered && dx > touchSlop * 2) {
                                swipeTriggered = true
                                onEdgeSwipeRight?.invoke()
                            }
                            return true
                        }
                        DragAction.HORIZONTAL_SEEK -> {
                            onHorizontalSeek?.invoke(dx, false)
                            return true
                        }
                        DragAction.NONE -> {
                            // Waiting to exceed touchSlop
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                val wasAction = dragAction
                val dx = abs(event.x - downX)
                val dy = abs(event.y - downY)

                if (wasAction == DragAction.HORIZONTAL_SEEK) {
                    onHorizontalSeek?.invoke(event.x - downX, true)
                    dragAction = DragAction.NONE
                    return true
                }

                dragAction = DragAction.NONE
                if (wasAction == DragAction.NONE && dx <= touchSlop && dy <= touchSlop) {
                    onSingleTap?.invoke()
                    performClick()
                    return true
                }
                if (wasAction != DragAction.NONE) {
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (dragAction == DragAction.HORIZONTAL_SEEK) {
                    onHorizontalSeek?.invoke(0f, true)
                }
                dragAction = DragAction.NONE
                swipeTriggered = false
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
        if (windowLevel in 0f..1f) return windowLevel
        val systemLevel = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(128)
        return (systemLevel / 255f).coerceIn(0.01f, 1f)
    }

    private fun setBrightness(level: Float) {
        val window = currentWindow() ?: return
        if (originalWindowBrightness == null) originalWindowBrightness = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = level.coerceIn(0.01f, 1f) }
    }

    private fun currentVolume(): Float {
        val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maximum.toFloat()).coerceIn(0f, 1f)
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

package com.fortresstv.player.ui

import android.view.KeyEvent

enum class TvPlayerAction {
    TOGGLE_CONTROLS,
    CHANNEL_UP,
    CHANNEL_DOWN,
    SEEK_BACKWARD,
    SEEK_FORWARD,
    PLAY,
    PAUSE,
    TOGGLE_PLAY_PAUSE,
    STOP,
}

object TvPlayerKeyDispatcher {
    /**
     * Maps an Android KeyEvent to a TvPlayerAction, or null if unhandled.
     */
    fun dispatch(keyCode: Int, isActionDown: Boolean, isLiveStream: Boolean = true): TvPlayerAction? {
        if (!isActionDown) return null

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> TvPlayerAction.TOGGLE_CONTROLS

            KeyEvent.KEYCODE_CHANNEL_UP -> TvPlayerAction.CHANNEL_UP
            KeyEvent.KEYCODE_DPAD_UP -> if (isLiveStream) TvPlayerAction.CHANNEL_UP else null

            KeyEvent.KEYCODE_CHANNEL_DOWN -> TvPlayerAction.CHANNEL_DOWN
            KeyEvent.KEYCODE_DPAD_DOWN -> if (isLiveStream) TvPlayerAction.CHANNEL_DOWN else null

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> TvPlayerAction.SEEK_BACKWARD

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> TvPlayerAction.SEEK_FORWARD

            KeyEvent.KEYCODE_MEDIA_PLAY -> TvPlayerAction.PLAY
            KeyEvent.KEYCODE_MEDIA_PAUSE -> TvPlayerAction.PAUSE
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> TvPlayerAction.TOGGLE_PLAY_PAUSE

            KeyEvent.KEYCODE_MEDIA_STOP -> TvPlayerAction.STOP

            else -> null
        }
    }
}

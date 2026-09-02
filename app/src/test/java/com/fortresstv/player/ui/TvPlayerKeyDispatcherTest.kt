package com.fortresstv.player.ui

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvPlayerKeyDispatcherTest {

    @Test
    fun mapsCenterAndEnterToToggleControls() {
        assertEquals(TvPlayerAction.TOGGLE_CONTROLS, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_DPAD_CENTER, true))
        assertEquals(TvPlayerAction.TOGGLE_CONTROLS, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_ENTER, true))
    }

    @Test
    fun mapsChannelUpAndDown() {
        assertEquals(TvPlayerAction.CHANNEL_UP, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_CHANNEL_UP, true))
        assertEquals(TvPlayerAction.CHANNEL_UP, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_DPAD_UP, true, isLiveStream = true))

        assertEquals(TvPlayerAction.CHANNEL_DOWN, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_CHANNEL_DOWN, true))
        assertEquals(TvPlayerAction.CHANNEL_DOWN, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_DPAD_DOWN, true, isLiveStream = true))
    }

    @Test
    fun mapsSeekingKeysForVod() {
        assertEquals(TvPlayerAction.SEEK_BACKWARD, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_DPAD_LEFT, true, isLiveStream = false))
        assertEquals(TvPlayerAction.SEEK_BACKWARD, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_MEDIA_REWIND, true, isLiveStream = false))

        assertEquals(TvPlayerAction.SEEK_FORWARD, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_DPAD_RIGHT, true, isLiveStream = false))
        assertEquals(TvPlayerAction.SEEK_FORWARD, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, true, isLiveStream = false))
    }

    @Test
    fun vodIgnoresDpadUpDownForChannelSwitch() {
        assertNull(TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_DPAD_UP, true, isLiveStream = false))
        assertNull(TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_DPAD_DOWN, true, isLiveStream = false))
    }

    @Test
    fun mapsMediaControls() {
        assertEquals(TvPlayerAction.PLAY, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_MEDIA_PLAY, true))
        assertEquals(TvPlayerAction.PAUSE, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_MEDIA_PAUSE, true))
        assertEquals(TvPlayerAction.TOGGLE_PLAY_PAUSE, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true))
        assertEquals(TvPlayerAction.STOP, TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_MEDIA_STOP, true))
    }

    @Test
    fun ignoresKeyUpEvents() {
        assertNull(TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_DPAD_CENTER, false))
        assertNull(TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_CHANNEL_UP, false))
    }

    @Test
    fun ignoresUnrelatedKeys() {
        assertNull(TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_A, true))
        assertNull(TvPlayerKeyDispatcher.dispatch(KeyEvent.KEYCODE_VOLUME_UP, true))
    }
}

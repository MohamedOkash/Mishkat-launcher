package com.mishkat.launcher.input

import android.view.KeyEvent
import com.mishkat.launcher.core.Logger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** R19: mapping table tests for XiaomiRemoteAdapter, GenericChineseAdapter, AirMouseAdapter, and RemoteUnificationLayer. */
class RemoteAdaptersTest {

    @Before
    fun setUp() {
        Logger.enabled = false
    }

    @Test
    fun xiaomiRemoteMappingTable() {
        val expected = mapOf(
            KeyEvent.KEYCODE_DPAD_UP to MishkatAction.NavigateUp,
            KeyEvent.KEYCODE_DPAD_DOWN to MishkatAction.NavigateDown,
            KeyEvent.KEYCODE_DPAD_LEFT to MishkatAction.NavigateLeft,
            KeyEvent.KEYCODE_DPAD_RIGHT to MishkatAction.NavigateRight,
            KeyEvent.KEYCODE_DPAD_CENTER to MishkatAction.Confirm,
            KeyEvent.KEYCODE_ENTER to MishkatAction.Confirm,
            KeyEvent.KEYCODE_BACK to MishkatAction.Back,
            KeyEvent.KEYCODE_HOME to MishkatAction.Home,
            KeyEvent.KEYCODE_MENU to MishkatAction.Menu,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to MishkatAction.PlayPause,
            KeyEvent.KEYCODE_MEDIA_NEXT to MishkatAction.Next,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS to MishkatAction.Previous,
            KeyEvent.KEYCODE_VOLUME_UP to MishkatAction.VolumeUp,
            KeyEvent.KEYCODE_VOLUME_DOWN to MishkatAction.VolumeDown,
            KeyEvent.KEYCODE_VOLUME_MUTE to MishkatAction.Mute,
            KeyEvent.KEYCODE_SEARCH to MishkatAction.Search,
            KeyEvent.KEYCODE_TV_DATA_SERVICE to MishkatAction.Settings,
        )
        for ((code, action) in expected) {
            assertEquals("keyCode=$code", action, XiaomiRemoteAdapter.translate(code, null))
        }
        assertNull(XiaomiRemoteAdapter.translate(KeyEvent.KEYCODE_F12, null))
    }

    @Test
    fun genericChineseMappingTable() {
        assertEquals(MishkatAction.Confirm, GenericChineseAdapter.translate(KeyEvent.KEYCODE_NUMPAD_ENTER, null))
        assertEquals(MishkatAction.PlayPause, GenericChineseAdapter.translate(KeyEvent.KEYCODE_MEDIA_PLAY, null))
        assertEquals(MishkatAction.PlayPause, GenericChineseAdapter.translate(KeyEvent.KEYCODE_MEDIA_PAUSE, null))
        assertEquals(MishkatAction.Menu, GenericChineseAdapter.translate(KeyEvent.KEYCODE_SETTINGS, null))

        for (d in 0..9) {
            assertEquals(
                MishkatAction.ChannelNumber(d),
                GenericChineseAdapter.translate(KeyEvent.KEYCODE_0 + d, null),
            )
            assertEquals(
                MishkatAction.ChannelNumber(d),
                GenericChineseAdapter.translate(KeyEvent.KEYCODE_NUMPAD_0 + d, null),
            )
        }
        assertNull(GenericChineseAdapter.translate(KeyEvent.KEYCODE_F11, null))
    }

    @Test
    fun airMouseMappingTable() {
        assertEquals(MishkatAction.Confirm, AirMouseAdapter.translate(KeyEvent.KEYCODE_BUTTON_1, null))
        assertEquals(MishkatAction.Back, AirMouseAdapter.translate(KeyEvent.KEYCODE_BUTTON_2, null))
        assertNull(AirMouseAdapter.translate(KeyEvent.KEYCODE_BUTTON_3, null))
    }

    @Test
    fun unificationLayerPassThroughAndConsumption() {
        // Native D-pad passes through so Compose focus engine handles it (D-001).
        assertEquals(
            RemoteUnificationLayer.PASS,
            RemoteUnificationLayer.dispatch(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.ACTION_DOWN, 0),
        )
        // Non-native confirm (AirMouse BUTTON_1) is remapped to canonical DPAD_CENTER.
        assertEquals(
            KeyEvent.KEYCODE_DPAD_CENTER,
            RemoteUnificationLayer.dispatch(KeyEvent.KEYCODE_BUTTON_1, KeyEvent.ACTION_DOWN, 0),
        )
        // App-only action (MENU) is consumed on both DOWN and UP.
        assertEquals(
            RemoteUnificationLayer.CONSUMED,
            RemoteUnificationLayer.dispatch(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_DOWN, 0),
        )
        assertEquals(
            RemoteUnificationLayer.CONSUMED,
            RemoteUnificationLayer.dispatch(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_UP, 0),
        )
        // Repeat on Confirm is suppressed; repeat on D-pad is allowed.
        assertEquals(
            RemoteUnificationLayer.CONSUMED,
            RemoteUnificationLayer.dispatch(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_DOWN, 3),
        )
        assertEquals(
            RemoteUnificationLayer.PASS,
            RemoteUnificationLayer.dispatch(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN, 3),
        )
    }

    @Test
    fun unknownKeysAreCountedForMissingButtonReport() {
        val weirdKey = 9991
        repeat(5) {
            assertEquals(
                RemoteUnificationLayer.PASS,
                RemoteUnificationLayer.dispatch(weirdKey, KeyEvent.ACTION_DOWN, 0),
            )
        }
        assertTrue((RemoteUnificationLayer.unknownReport()[weirdKey] ?: 0) >= 5)
    }
}

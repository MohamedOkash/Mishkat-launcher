package com.mishkat.launcher.input

import android.view.KeyEvent

/**
 * Translates a raw key code into a [MishkatAction]. Returns null if this adapter does not know the key.
 * Rule (§7): a new remote = a new adapter only, zero UI changes.
 *
 * D-001: [event] is nullable so mapping tables are unit-testable on the JVM; adapters must rely on
 * [keyCode] and only use [event] for optional extras (device id, scan code).
 */
interface RemoteAdapter {
    val name: String
    fun translate(keyCode: Int, event: KeyEvent?): MishkatAction?
}

/** Mi Box S 2 BT remote (baseline device). */
object XiaomiRemoteAdapter : RemoteAdapter {
    override val name = "Xiaomi"

    override fun translate(keyCode: Int, event: KeyEvent?): MishkatAction? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> MishkatAction.NavigateUp
        KeyEvent.KEYCODE_DPAD_DOWN -> MishkatAction.NavigateDown
        KeyEvent.KEYCODE_DPAD_LEFT -> MishkatAction.NavigateLeft
        KeyEvent.KEYCODE_DPAD_RIGHT -> MishkatAction.NavigateRight
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> MishkatAction.Confirm
        KeyEvent.KEYCODE_BACK -> MishkatAction.Back
        KeyEvent.KEYCODE_HOME -> MishkatAction.Home
        KeyEvent.KEYCODE_MENU -> MishkatAction.Menu
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> MishkatAction.PlayPause
        KeyEvent.KEYCODE_MEDIA_NEXT -> MishkatAction.Next
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MishkatAction.Previous
        KeyEvent.KEYCODE_VOLUME_UP -> MishkatAction.VolumeUp
        KeyEvent.KEYCODE_VOLUME_DOWN -> MishkatAction.VolumeDown
        KeyEvent.KEYCODE_VOLUME_MUTE -> MishkatAction.Mute
        KeyEvent.KEYCODE_SEARCH -> MishkatAction.Search
        KeyEvent.KEYCODE_TV_DATA_SERVICE -> MishkatAction.Settings
        else -> null
    }
}

/** Generic Chinese IR boxes: numpad, separate play/pause keys, digits, settings key. */
object GenericChineseAdapter : RemoteAdapter {
    override val name = "GenericChinese"

    override fun translate(keyCode: Int, event: KeyEvent?): MishkatAction? = when (keyCode) {
        KeyEvent.KEYCODE_NUMPAD_ENTER -> MishkatAction.Confirm
        KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> MishkatAction.PlayPause
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> MishkatAction.ChannelNumber(keyCode - KeyEvent.KEYCODE_0)
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 ->
            MishkatAction.ChannelNumber(keyCode - KeyEvent.KEYCODE_NUMPAD_0)
        KeyEvent.KEYCODE_SETTINGS -> MishkatAction.Menu
        else -> null
    }
}

/** Air-mouse remotes that send gamepad-style buttons. */
object AirMouseAdapter : RemoteAdapter {
    override val name = "AirMouse"

    override fun translate(keyCode: Int, event: KeyEvent?): MishkatAction? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_1 -> MishkatAction.Confirm
        KeyEvent.KEYCODE_BUTTON_2 -> MishkatAction.Back
        else -> null
    }
}

/**
 * S8: Smart TVs with built-in Satellite/Terrestrial Receiver (Android 7.0+ TCL, Toshiba, Tornado,
 * Fresh, Sharp, Skyworth, Hisense, Sony, Philips) & DVB-S2 combo boxes.
 * Maps TV/Live, Source/Input, CH+/CH-, Guide/Info, and the 4 color buttons (Red/Green/Yellow/Blue).
 */
object SmartTvReceiverAdapter : RemoteAdapter {
    override val name = "SmartTvReceiver"

    override fun translate(keyCode: Int, event: KeyEvent?): MishkatAction? = when (keyCode) {
        KeyEvent.KEYCODE_TV,
        KeyEvent.KEYCODE_TV_SATELLITE,
        KeyEvent.KEYCODE_TV_SATELLITE_BS,
        KeyEvent.KEYCODE_TV_SATELLITE_CS,
        KeyEvent.KEYCODE_TV_SATELLITE_SERVICE,
        KeyEvent.KEYCODE_TV_ANTENNA_CABLE,
        KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG,
        KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL,
        KeyEvent.KEYCODE_GUIDE,
        KeyEvent.KEYCODE_DVR -> MishkatAction.TvLive

        KeyEvent.KEYCODE_TV_INPUT,
        KeyEvent.KEYCODE_TV_INPUT_HDMI_1,
        KeyEvent.KEYCODE_TV_INPUT_HDMI_2,
        KeyEvent.KEYCODE_TV_INPUT_HDMI_3,
        KeyEvent.KEYCODE_TV_INPUT_HDMI_4,
        KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1,
        KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2,
        KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1,
        KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2,
        KeyEvent.KEYCODE_AVR_INPUT -> MishkatAction.TvInput

        KeyEvent.KEYCODE_CHANNEL_UP,
        KeyEvent.KEYCODE_PAGE_UP -> MishkatAction.ChannelUp

        KeyEvent.KEYCODE_CHANNEL_DOWN,
        KeyEvent.KEYCODE_PAGE_DOWN -> MishkatAction.ChannelDown

        KeyEvent.KEYCODE_PROG_RED -> MishkatAction.ColorRed
        KeyEvent.KEYCODE_PROG_GREEN -> MishkatAction.ColorGreen
        KeyEvent.KEYCODE_PROG_YELLOW -> MishkatAction.ColorYellow
        KeyEvent.KEYCODE_PROG_BLUE -> MishkatAction.ColorBlue
        KeyEvent.KEYCODE_INFO -> MishkatAction.Settings
        else -> null
    }
}

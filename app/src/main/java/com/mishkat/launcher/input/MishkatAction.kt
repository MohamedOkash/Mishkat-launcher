package com.mishkat.launcher.input

/**
 * Unified remote actions (§7, corrected per D-001: each object declared separately).
 */
sealed class MishkatAction {
    data object NavigateUp : MishkatAction()
    data object NavigateDown : MishkatAction()
    data object NavigateLeft : MishkatAction()
    data object NavigateRight : MishkatAction()
    data object Confirm : MishkatAction()
    data object Back : MishkatAction()
    data object Home : MishkatAction()
    data object Menu : MishkatAction()
    data object PlayPause : MishkatAction()
    data object Next : MishkatAction()
    data object Previous : MishkatAction()
    data object VolumeUp : MishkatAction()
    data object VolumeDown : MishkatAction()
    data object Mute : MishkatAction()
    data object Search : MishkatAction()
    data object Settings : MishkatAction()
    /** S8: Dedicated TV / Satellite / Receiver & HDMI Input keys + 4 Color buttons on TV remotes. */
    data object TvLive : MishkatAction()
    data object TvInput : MishkatAction()
    data object ChannelUp : MishkatAction()
    data object ChannelDown : MishkatAction()
    data object ColorRed : MishkatAction()
    data object ColorGreen : MishkatAction()
    data object ColorYellow : MishkatAction()
    data object ColorBlue : MishkatAction()
    data class ChannelNumber(val digit: Int) : MishkatAction()
    data class Unknown(val keyCode: Int) : MishkatAction()

    /**
     * D-001: actions the system / Compose focus engine must still receive.
     * The layer only observes them (emits to the flow) and returns false from dispatch.
     */
    val isPassThrough: Boolean
        get() = when (this) {
            NavigateUp, NavigateDown, NavigateLeft, NavigateRight,
            Confirm, Back, VolumeUp, VolumeDown, Mute -> true
            else -> false
        }

    /** D-001: long-press repeats are allowed for these (fast shelf scrolling, volume). */
    val allowsRepeat: Boolean
        get() = when (this) {
            NavigateUp, NavigateDown, NavigateLeft, NavigateRight, VolumeUp, VolumeDown -> true
            else -> false
        }
}

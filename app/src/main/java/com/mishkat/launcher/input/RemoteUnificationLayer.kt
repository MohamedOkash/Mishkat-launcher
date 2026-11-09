package com.mishkat.launcher.input

import android.view.KeyEvent
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * §7 v1.1 + D-001.
 *
 * - Every recognised key-down is emitted on [actions] with tryEmit (R5: never blocks).
 * - Pass-through actions (D-pad, OK, Back, Volume) are NOT consumed so the Compose focus engine and
 *   the system still handle them. Non-standard keys mapped to them (e.g. AirMouse BUTTON_1) are
 *   remapped to the canonical key code so Compose understands them.
 * - App actions (Menu, media, digits, Search, Settings) are consumed, both DOWN and UP.
 * - Long-press repeats are allowed for navigation/volume only.
 *
 * MainActivity: see dispatchKeyEvent.
 */
object RemoteUnificationLayer {

    /** [dispatch] result: let the event continue unchanged. */
    const val PASS = 0

    /** [dispatch] result: event fully handled; stop propagation. */
    const val CONSUMED = -1
    // Any positive result = re-dispatch the event with that key code.

    private const val UNKNOWN_REPORT_THRESHOLD = 5

    private val adapters: List<RemoteAdapter> = listOf(
        XiaomiRemoteAdapter,
        GenericChineseAdapter,
        AirMouseAdapter,
        SmartTvReceiverAdapter,
    )

    private val _actions = MutableSharedFlow<MishkatAction>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val actions: SharedFlow<MishkatAction> = _actions.asSharedFlow()

    /** Key codes the platform / Compose already understand natively. */
    private val nativeKeys = intArrayOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
    )

    private val unknownCounts = HashMap<Int, Int>()

    fun emitAction(action: MishkatAction) {
        _actions.tryEmit(action)
    }

    fun dispatch(event: KeyEvent): Int =
        dispatch(event.keyCode, event.action, event.repeatCount, event)

    /** Pure core, unit-testable on the JVM. */
    fun dispatch(keyCode: Int, action: Int, repeatCount: Int, event: KeyEvent? = null): Int {
        if (action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_UP) return PASS

        var adapterName = ""
        var translated: MishkatAction? = null
        for (i in adapters.indices) {
            val a = adapters[i]
            val t = a.translate(keyCode, event)
            if (t != null) {
                translated = t
                adapterName = a.name
                break
            }
        }

        if (action == KeyEvent.ACTION_DOWN) {
            if (translated == null) {
                if (repeatCount == 0) {
                    onUnknown(keyCode)
                    _actions.tryEmit(MishkatAction.Unknown(keyCode))
                }
                return PASS
            }
            if (repeatCount > 0 && !translated.allowsRepeat) return CONSUMED
            if (repeatCount == 0) Logger.input { "$adapterName: $keyCode -> $translated" }
            _actions.tryEmit(translated)
        }

        if (translated == null) return PASS
        if (!translated.isPassThrough) return CONSUMED
        return if (keyCode in nativeKeys) PASS else canonicalKeyCode(translated)
    }

    /** Snapshot of unknown key codes and how often they were pressed (for a future "missing button" report). */
    fun unknownReport(): Map<Int, Int> = synchronized(unknownCounts) { HashMap(unknownCounts) }

    private fun onUnknown(keyCode: Int) {
        val count = synchronized(unknownCounts) {
            val c = (unknownCounts[keyCode] ?: 0) + 1
            unknownCounts[keyCode] = c
            c
        }
        if (count == UNKNOWN_REPORT_THRESHOLD) {
            Logger.w(Logger.TAG_INPUT) { "Missing button report: keyCode=$keyCode pressed $count times" }
        }
    }

    private fun canonicalKeyCode(a: MishkatAction): Int = when (a) {
        MishkatAction.NavigateUp -> KeyEvent.KEYCODE_DPAD_UP
        MishkatAction.NavigateDown -> KeyEvent.KEYCODE_DPAD_DOWN
        MishkatAction.NavigateLeft -> KeyEvent.KEYCODE_DPAD_LEFT
        MishkatAction.NavigateRight -> KeyEvent.KEYCODE_DPAD_RIGHT
        MishkatAction.Confirm -> KeyEvent.KEYCODE_DPAD_CENTER
        MishkatAction.Back -> KeyEvent.KEYCODE_BACK
        MishkatAction.Home -> KeyEvent.KEYCODE_HOME
        MishkatAction.VolumeUp -> KeyEvent.KEYCODE_VOLUME_UP
        MishkatAction.VolumeDown -> KeyEvent.KEYCODE_VOLUME_DOWN
        MishkatAction.Mute -> KeyEvent.KEYCODE_VOLUME_MUTE
        else -> PASS
    }
}

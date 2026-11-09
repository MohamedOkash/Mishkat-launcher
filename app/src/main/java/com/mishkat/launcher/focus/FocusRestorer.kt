package com.mishkat.launcher.focus

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Remembers the last focused element id (DataStore) so focus returns to the same card after a
 * process death / crash / reboot.
 *
 * Flow: [start] loads the stored id into [restoreTarget]. Each [mishkatFocus] node with a matching id
 * claims it once via [claim]. If nothing claims it (e.g. the app was uninstalled) the screen falls back
 * to its default focus.
 */
class FocusRestorer(
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    val restoreTarget = CompletableDeferred<String?>()

    @Volatile
    private var pending: String? = null

    private val lastFocused = MutableStateFlow<String?>(null)

    /** True once the stored id has been claimed or there was nothing to restore. */
    val isSettled: Boolean
        get() = restoreTarget.isCompleted && pending == null

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            val stored = runCatching { prefs.data.first()[KEY] }.getOrNull()
            pending = stored
            restoreTarget.complete(stored)
            Logger.focus { "Restore target = $stored" }
            lastFocused.filterNotNull().debounce(PERSIST_DEBOUNCE_MS).collect { id ->
                prefs.edit { it[KEY] = id }
            }
        }
    }

    /** Called by focus nodes on focus gain. Allocation-free (stable id strings). */
    fun onFocused(id: String) {
        lastFocused.value = id
    }

    /** Returns true exactly once, for the node whose id matches the stored target. */
    fun claim(id: String): Boolean {
        if (pending != id) return false
        pending = null
        return true
    }

    /** Give up on restoring (target not present on screen). */
    fun abandon() {
        pending = null
    }

    private companion object {
        val KEY = stringPreferencesKey("last_focus_id")
        const val PERSIST_DEBOUNCE_MS = 800L
    }
}

val LocalFocusRestorer = staticCompositionLocalOf<FocusRestorer?> { null }

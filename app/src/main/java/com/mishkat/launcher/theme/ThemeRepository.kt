package com.mishkat.launcher.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads built-in theme presets from assets/themes/ (offline, R3) and persists the selection.
 * Starts with [ThemeTokens.Default] so the first frame never waits on IO (R5).
 */
class ThemeRepository(
    private val context: Context,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = false }

    private val _themes = MutableStateFlow<List<ThemeSpec>>(emptyList())
    val themes: StateFlow<List<ThemeSpec>> = _themes.asStateFlow()

    private val _tokens = MutableStateFlow(ThemeTokens.Default)
    val tokens: StateFlow<ThemeTokens> = _tokens.asStateFlow()

    fun start() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { loadBuiltins() }
            _themes.value = loaded
            val selectedId = prefs.data.first()[KEY_THEME] ?: ThemeTokens.Default.id
            apply(selectedId)
        }
    }

    fun select(id: String) {
        scope.launch {
            apply(id)
            prefs.edit { it[KEY_THEME] = id }
        }
    }

    private fun apply(id: String) {
        val spec = _themes.value.firstOrNull { it.id == id } ?: return
        _tokens.value = runCatching { ThemeTokens.fromSpec(spec) }
            .onFailure { t -> Logger.e(Logger.TAG_THEME, t) { "Theme '$id' invalid, keeping current" } }
            .getOrDefault(_tokens.value)
        Logger.theme { "Applied theme ${_tokens.value.id}" }
    }

    private fun loadBuiltins(): List<ThemeSpec> {
        val files = context.assets.list(ASSET_DIR).orEmpty().filter { it.endsWith(".json") }.sorted()
        return files.mapNotNull { file ->
            runCatching {
                context.assets.open("$ASSET_DIR/$file").bufferedReader().use { r ->
                    json.decodeFromString(ThemeSpec.serializer(), r.readText())
                }
            }.onFailure { t -> Logger.e(Logger.TAG_THEME, t) { "Failed to parse $file" } }.getOrNull()
        }
    }

    private companion object {
        const val ASSET_DIR = "themes"
        val KEY_THEME = stringPreferencesKey("theme_id")
    }
}

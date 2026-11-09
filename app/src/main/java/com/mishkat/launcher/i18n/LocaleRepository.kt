package com.mishkat.launcher.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mishkat.launcher.core.Logger
import com.mishkat.launcher.theme.WALLPAPER_PRESETS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

@Immutable
data class AppSettings(
    val language: String = "ar",
    val use24Hour: Boolean = false,
    val launchOnBoot: Boolean = true,
    val wallpaperStyle: String = "auto",
    val compactLayout: Boolean = true,
    val appsAtTop: Boolean = true,
    val glassIntensity: Float = 0.90f,
    val lockTimeoutMin: Int = 15,
    val isLightMode: Boolean = false,
    val fontScale: Float = 1.0f,
    val dataSaverMode: Boolean = true,
    val shelfOrder: List<String> = listOf("livetv", "apps", "prayer", "sections", "radio", "tv", "sports"),
) {
    val isArabic: Boolean
        get() = language != "en"

    val layoutDirection: LayoutDirection
        get() = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    val locale: Locale
        get() = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
}

/**
 * R21: In-app language switching (ar RTL / en LTR) without device restart,
 * locale-aware digit formatting, 12/24-hour clock toggle, and full launcher UI customization.
 */
object LocaleFormatter {
    private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun localizeDigits(input: String, isArabic: Boolean): String {
        if (!isArabic) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(ARABIC_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun localizedContext(base: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }
}

class LocaleRepository(
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun start() {
        scope.launch {
            val data = runCatching { prefs.data.first() }.getOrNull()
            val lang = data?.get(KEY_LANGUAGE) ?: "ar"
            val use24 = data?.get(KEY_USE_24H) ?: false
            val onBoot = data?.get(KEY_LAUNCH_ON_BOOT) ?: true
            val wp = data?.get(KEY_WALLPAPER_STYLE) ?: "auto"
            val compact = data?.get(KEY_COMPACT_LAYOUT) ?: true
            val appsTop = data?.get(KEY_APPS_AT_TOP) ?: true
            val glass = data?.get(KEY_GLASS_INTENSITY) ?: 0.90f
            val lockMin = data?.get(KEY_LOCK_TIMEOUT_MIN) ?: 15
            val lightMode = data?.get(KEY_LIGHT_MODE) ?: false
            val fScale = data?.get(KEY_FONT_SCALE) ?: 1.0f
            val dataSaver = data?.get(KEY_DATA_SAVER) ?: true
            val rawShelfOrder = data?.get(KEY_SHELF_ORDER)
            val parsedOrder = rawShelfOrder?.split(",")?.filter { it.isNotBlank() } ?: listOf("livetv", "apps", "prayer", "sections", "radio", "tv", "sports")

            val initial = AppSettings(
                language = lang,
                use24Hour = use24,
                launchOnBoot = onBoot,
                wallpaperStyle = wp,
                compactLayout = compact,
                appsAtTop = appsTop,
                glassIntensity = glass,
                lockTimeoutMin = lockMin,
                isLightMode = lightMode,
                fontScale = fScale,
                dataSaverMode = dataSaver,
                shelfOrder = parsedOrder,
            )
            Locale.setDefault(initial.locale)
            _settings.value = initial
            Logger.system { "Loaded AppSettings: lang=$lang, wp=$wp, compact=$compact, lightMode=$lightMode, dataSaver=$dataSaver, shelfOrder=$parsedOrder" }
        }
    }

    fun toggleLanguage() {
        val nextLang = if (_settings.value.isArabic) "en" else "ar"
        setLanguage(nextLang)
    }

    fun setLanguage(lang: String) {
        val normalized = if (lang == "en") "en" else "ar"
        val updated = _settings.value.copy(language = normalized)
        Locale.setDefault(updated.locale)
        _settings.value = updated
        scope.launch { prefs.edit { it[KEY_LANGUAGE] = normalized } }
        Logger.system { "Switched language in-app to $normalized" }
    }

    fun toggle24Hour() {
        val next = !_settings.value.use24Hour
        _settings.value = _settings.value.copy(use24Hour = next)
        scope.launch { prefs.edit { it[KEY_USE_24H] = next } }
    }

    fun toggleLaunchOnBoot() {
        val next = !_settings.value.launchOnBoot
        _settings.value = _settings.value.copy(launchOnBoot = next)
        scope.launch { prefs.edit { it[KEY_LAUNCH_ON_BOOT] = next } }
    }

    fun setWallpaperStyle(styleId: String) {
        _settings.value = _settings.value.copy(wallpaperStyle = styleId)
        scope.launch { prefs.edit { it[KEY_WALLPAPER_STYLE] = styleId } }
    }

    fun cycleWallpaperStyle() {
        val ids = WALLPAPER_PRESETS.map { it.id }
        val idx = ids.indexOf(_settings.value.wallpaperStyle)
        val nextId = ids[(idx + 1) % ids.size]
        setWallpaperStyle(nextId)
    }

    fun toggleCompactLayout() {
        val next = !_settings.value.compactLayout
        _settings.value = _settings.value.copy(compactLayout = next)
        scope.launch { prefs.edit { it[KEY_COMPACT_LAYOUT] = next } }
    }

    fun toggleAppsAtTop() {
        val next = !_settings.value.appsAtTop
        _settings.value = _settings.value.copy(appsAtTop = next)
        scope.launch { prefs.edit { it[KEY_APPS_AT_TOP] = next } }
    }

    fun cycleGlassIntensity() {
        val current = _settings.value.glassIntensity
        val next = when {
            current >= 0.85f -> 0.55f
            current >= 0.50f -> 1.10f
            else -> 0.90f
        }
        _settings.value = _settings.value.copy(glassIntensity = next)
        scope.launch { prefs.edit { it[KEY_GLASS_INTENSITY] = next } }
    }

    fun cycleLockTimeout() {
        val presets = listOf(0, 5, 15, 30)
        val idx = presets.indexOf(_settings.value.lockTimeoutMin)
        val next = presets[(idx + 1) % presets.size]
        _settings.value = _settings.value.copy(lockTimeoutMin = next)
        scope.launch { prefs.edit { it[KEY_LOCK_TIMEOUT_MIN] = next } }
    }

    fun toggleLightMode() {
        val next = !_settings.value.isLightMode
        _settings.value = _settings.value.copy(isLightMode = next)
        scope.launch { prefs.edit { it[KEY_LIGHT_MODE] = next } }
    }

    fun cycleFontScale() {
        val cur = _settings.value.fontScale
        val next = when {
            cur < 0.96f -> 1.0f
            cur < 1.08f -> 1.14f
            else -> 0.92f
        }
        _settings.value = _settings.value.copy(fontScale = next)
        scope.launch { prefs.edit { it[KEY_FONT_SCALE] = next } }
    }

    fun toggleDataSaver() {
        val next = !_settings.value.dataSaverMode
        _settings.value = _settings.value.copy(dataSaverMode = next)
        scope.launch { prefs.edit { it[KEY_DATA_SAVER] = next } }
        Logger.system { "Data Saver mode toggled: $next" }
    }

    fun moveShelfUp(id: String) {
        val list = _settings.value.shelfOrder.toMutableList()
        val idx = list.indexOf(id)
        if (idx > 0) {
            val item = list.removeAt(idx)
            list.add(idx - 1, item)
            _settings.value = _settings.value.copy(shelfOrder = list)
            scope.launch { prefs.edit { it[KEY_SHELF_ORDER] = list.joinToString(",") } }
        }
    }

    fun moveShelfDown(id: String) {
        val list = _settings.value.shelfOrder.toMutableList()
        val idx = list.indexOf(id)
        if (idx in 0 until list.lastIndex) {
            val item = list.removeAt(idx)
            list.add(idx + 1, item)
            _settings.value = _settings.value.copy(shelfOrder = list)
            scope.launch { prefs.edit { it[KEY_SHELF_ORDER] = list.joinToString(",") } }
        }
    }

    fun resetShelfOrder() {
        val defaultList = listOf("livetv", "apps", "prayer", "sections", "radio", "tv", "sports")
        _settings.value = _settings.value.copy(shelfOrder = defaultList)
        scope.launch { prefs.edit { it[KEY_SHELF_ORDER] = defaultList.joinToString(",") } }
    }

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_USE_24H = booleanPreferencesKey("use_24_hour")
        val KEY_LAUNCH_ON_BOOT = booleanPreferencesKey("launch_on_boot")
        val KEY_WALLPAPER_STYLE = stringPreferencesKey("wallpaper_style")
        val KEY_COMPACT_LAYOUT = booleanPreferencesKey("compact_layout")
        val KEY_APPS_AT_TOP = booleanPreferencesKey("apps_at_top")
        val KEY_GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val KEY_LOCK_TIMEOUT_MIN = intPreferencesKey("lock_timeout_min")
        val KEY_LIGHT_MODE = booleanPreferencesKey("is_light_mode")
        val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        val KEY_DATA_SAVER = booleanPreferencesKey("data_saver_mode")
        val KEY_SHELF_ORDER = stringPreferencesKey("shelf_order")
    }
}

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

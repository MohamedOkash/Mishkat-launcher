package com.mishkat.launcher.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.Collator

@Immutable
data class AppEntry(
    /** Stable key: flattened component name. Also used as LazyRow key and focus id. */
    val key: String,
    val packageName: String,
    val label: String,
    val component: ComponentName,
    /** TV banner (16:9) if the app provides one, else null. */
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
)

sealed interface AppsState {
    data object Loading : AppsState
    data class Ready(
        /** Visible apps sorted with pinned first, then alphabetically. */
        val visibleApps: List<AppEntry>,
        /** Complete list including hidden apps (for the App Drawer / Bloatware Manager). */
        val allApps: List<AppEntry>,
    ) : AppsState
    data class Failed(val message: String) : AppsState
}

/** Pure sorting & filtering policy, unit-tested per R19. */
object AppListPolicy {
    fun organize(
        raw: List<AppEntry>,
        pinnedPackages: Set<String>,
        hiddenPackages: Set<String>,
        customOrder: List<String> = emptyList(),
    ): Pair<List<AppEntry>, List<AppEntry>> {
        val orderMap = customOrder.withIndex().associate { (idx, pkg) -> pkg to idx }
        val annotated = raw.map { entry ->
            entry.copy(
                isPinned = entry.packageName in pinnedPackages,
                isHidden = entry.packageName in hiddenPackages,
            )
        }.sortedWith(
            compareByDescending<AppEntry> { it.isPinned }
                .thenBy { orderMap[it.packageName] ?: Int.MAX_VALUE }
                .thenBy { it.label.lowercase() },
        )
        val visible = annotated.filter { !it.isHidden }
        return visible to annotated
    }
}

/**
 * Real installed apps from PackageManager (LEANBACK_LAUNCHER first, then LAUNCHER).
 * - Supports pinning favorite apps to the front and hiding unused/ad-heavy bloatware apps (S4).
 * - Caches rasterized icons/banners on disk (`filesDir/icon_cache/`) keyed by package `lastUpdateTime`
 *   so subsequent cold starts skip slow Drawable rasterization (S5 §10).
 */
class AppsRepository(
    private val context: Context,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<AppsState>(AppsState.Loading)
    val state: StateFlow<AppsState> = _state.asStateFlow()

    private val iconCacheDir by lazy {
        File(context.filesDir, "icon_cache").apply { mkdirs() }
    }

    private var rawApps: List<AppEntry> = emptyList()
    private var pinnedSet: Set<String> = emptySet()
    private var hiddenSet: Set<String> = emptySet()
    private var customOrder: List<String> = emptyList()
    private var loadJob: Job? = null

    fun start() {
        scope.launch {
            val data = runCatching { prefs.data.first() }.getOrNull()
            pinnedSet = data?.get(KEY_PINNED).orEmpty()
            hiddenSet = data?.get(KEY_HIDDEN).orEmpty()
            customOrder = data?.get(KEY_CUSTOM_ORDER)
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            reload()
        }

        runCatching {
            context.getSystemService(LauncherApps::class.java)?.registerCallback(object : LauncherApps.Callback() {
                override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
                override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
                override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
                override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
                override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
            })
        }.onFailure { t -> Logger.w(Logger.TAG_HOME, t) { "LauncherApps callback unavailable" } }
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.IO) {
            _state.value = runCatching {
                rawApps = query()
                val (visible, all) = AppListPolicy.organize(rawApps, pinnedSet, hiddenSet, customOrder)
                AppsState.Ready(visibleApps = visible, allApps = all)
            }.getOrElse { t ->
                Logger.e(Logger.TAG_HOME, t) { "App query failed" }
                AppsState.Failed(t.javaClass.simpleName)
            }
        }
    }

    fun togglePin(packageName: String) {
        pinnedSet = if (packageName in pinnedSet) pinnedSet - packageName else pinnedSet + packageName
        reorganizeInMemory()
        scope.launch { prefs.edit { it[KEY_PINNED] = pinnedSet } }
    }

    fun toggleHide(packageName: String) {
        hiddenSet = if (packageName in hiddenSet) hiddenSet - packageName else hiddenSet + packageName
        reorganizeInMemory()
        scope.launch { prefs.edit { it[KEY_HIDDEN] = hiddenSet } }
    }

    /**
     * Moves [packageName] earlier (`delta = -1`) or later (`delta = +1`) among visible apps
     * and persists the new custom sequence.
     */
    fun moveApp(packageName: String, delta: Int) {
        val currentVisible = (_state.value as? AppsState.Ready)?.visibleApps ?: return
        val pkgs = currentVisible.map { it.packageName }.toMutableList()
        val idx = pkgs.indexOf(packageName)
        if (idx < 0) return
        val targetIdx = (idx + delta).coerceIn(0, pkgs.lastIndex)
        if (targetIdx == idx) return
        val item = pkgs.removeAt(idx)
        pkgs.add(targetIdx, item)
        customOrder = pkgs
        reorganizeInMemory()
        val csv = customOrder.joinToString(",")
        scope.launch { prefs.edit { it[KEY_CUSTOM_ORDER] = csv } }
    }

    private fun reorganizeInMemory() {
        if (rawApps.isEmpty()) return
        val (visible, all) = AppListPolicy.organize(rawApps, pinnedSet, hiddenSet, customOrder)
        _state.value = AppsState.Ready(visibleApps = visible, allApps = all)
    }

    private fun query(): List<AppEntry> {
        val pm = context.packageManager
        val self = context.packageName
        val seen = HashSet<String>()
        val result = ArrayList<AppEntry>()

        fun collect(category: String) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val ai = ri.activityInfo ?: continue
                if (ai.packageName == self) continue
                if (!seen.add(ai.packageName)) continue
                result += toEntry(pm, ri)
            }
        }
        collect(Intent.CATEGORY_LEANBACK_LAUNCHER)
        collect(Intent.CATEGORY_LAUNCHER)

        val collator = Collator.getInstance()
        result.sortWith { a, b -> collator.compare(a.label, b.label) }
        Logger.home { "Loaded ${result.size} apps (user ${Process.myUserHandle()})" }
        return result
    }

    private fun toEntry(pm: PackageManager, ri: ResolveInfo): AppEntry {
        val ai = ri.activityInfo
        val pkg = ai.packageName
        val component = ComponentName(pkg, ai.name)
        val updateStamp = runCatching { pm.getPackageInfo(pkg, 0).lastUpdateTime }.getOrDefault(0L)

        val banner = loadOrCacheBitmap(
            cacheKey = "${pkg}_${updateStamp}_banner.png",
            w = BANNER_W,
            h = BANNER_H,
        ) {
            ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm)
        }

        val icon = if (banner == null) {
            loadOrCacheBitmap(
                cacheKey = "${pkg}_${updateStamp}_icon.png",
                w = ICON_PX,
                h = ICON_PX,
            ) {
                ri.loadIcon(pm)
            }
        } else {
            null
        }

        return AppEntry(
            key = component.flattenToString(),
            packageName = pkg,
            label = ri.loadLabel(pm).toString().ifBlank { pkg },
            component = component,
            banner = banner,
            icon = icon,
        )
    }

    private fun loadOrCacheBitmap(
        cacheKey: String,
        w: Int,
        h: Int,
        loader: () -> Drawable?,
    ): ImageBitmap? {
        val file = File(iconCacheDir, cacheKey)
        if (file.isFile && file.length() > 0L) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                .getOrNull()
                ?.let { return it }
        }
        val drawable = runCatching { loader() }.getOrNull() ?: return null
        return runCatching {
            val bmp = drawable.toBitmap(w, h)
            runCatching {
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
            }
            bmp.asImageBitmap()
        }.getOrNull()
    }

    private companion object {
        const val BANNER_W = 320
        const val BANNER_H = 180
        const val ICON_PX = 128
        val KEY_PINNED = stringSetPreferencesKey("pinned_packages")
        val KEY_HIDDEN = stringSetPreferencesKey("hidden_packages")
        val KEY_CUSTOM_ORDER = stringPreferencesKey("custom_app_order_csv")
    }
}

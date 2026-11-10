package com.mishkat.launcher.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.theme.ThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * §4 + §9 + R24: Honest Device Tools & Diagnostics panel.
 * Displays real kernel metrics (`/proc/meminfo`, `StatFs`, SoC thermal zone, and Mishkat's own PSS against the ≤120MB budget)
 * and performs a genuine measured cache cleanup showing exact bytes freed.
 */
@Composable
fun DeviceToolsOverlay(
    tokens: ThemeTokens,
    killer: BackgroundProcessKiller? = null,
    onOpenAppManager: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cleanButtonFocus = remember { FocusRequester() }

    val killerState by killer?.state?.collectAsState() ?: remember { mutableStateOf(BackgroundKillerState()) }
    var stats by remember { mutableStateOf(SystemStats()) }
    var lastCleanResult by remember { mutableStateOf<CleanResult?>(null) }
    var lastApkCleanResult by remember { mutableStateOf<CleanResult?>(null) }

    LaunchedEffect(Unit) {
        repeat(4) {
            withFrameNanos { }
            if (runCatching { cleanButtonFocus.requestFocus() }.isSuccess) return@repeat
        }
        while (true) {
            stats = withContext(Dispatchers.IO) { SystemStatsReader.readSnapshot(context) }
            delay(3_000L)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.surface.copy(alpha = 0.96f))
            .padding(horizontal = 56.dp, vertical = 36.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.tools_panel_title),
                        color = tokens.primary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.tools_honest_subtitle),
                        color = tokens.onSurfaceMuted,
                        fontSize = 14.sp,
                    )
                }

                val closeLabel = stringResource(R.string.radio_close)
                val pillShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "tools:close", radius = 14.dp)
                        .clip(pillShape)
                        .background(tokens.card)
                        .border(BorderStroke(1.dp, tokens.stroke), pillShape)
                        .clickable { onDismiss() }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .semantics { contentDescription = closeLabel },
                ) {
                    Text(text = closeLabel, color = tokens.onSurface, fontSize = 14.sp)
                }
            }

            // 4 Live Hardware & Budget Gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MetricGaugeCard(
                    modifier = Modifier.weight(1f),
                    tokens = tokens,
                    title = stringResource(R.string.tools_ram_title),
                    primaryValue = "${stats.ramUsedPercent}%",
                    detail = "${SystemStatsReader.formatBytes(stats.ramUsedBytes)} / ${SystemStatsReader.formatBytes(stats.ramTotalBytes)}",
                )

                MetricGaugeCard(
                    modifier = Modifier.weight(1f),
                    tokens = tokens,
                    title = stringResource(R.string.tools_storage_title),
                    primaryValue = "${stats.storageUsedPercent}%",
                    detail = stringResource(
                        R.string.tools_storage_free_fmt,
                        SystemStatsReader.formatBytes(stats.storageFreeBytes),
                    ),
                )

                val tempStr = stats.socTempCelsius?.let { String.format(Locale.US, "%.1f°C", it) } ?: "—"
                MetricGaugeCard(
                    modifier = Modifier.weight(1f),
                    tokens = tokens,
                    title = stringResource(R.string.tools_temp_title),
                    primaryValue = tempStr,
                    detail = "SoC Thermal",
                )

                MetricGaugeCard(
                    modifier = Modifier.weight(1f),
                    tokens = tokens,
                    title = stringResource(R.string.tools_launcher_ram_title),
                    primaryValue = "${stats.launcherPssMb} MB",
                    detail = stringResource(R.string.tools_budget_target),
                )
            }

            // Honest Cache Cleaner Action Card (R24)
            val cardShape = RoundedCornerShape(tokens.cardRadius)

            // Level 1: Background Process Killer & RAM Booster Action Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .mishkatFocus(id = "tools:sweep_background_apps")
                    .clip(cardShape)
                    .background(tokens.card)
                    .border(BorderStroke(1.dp, tokens.primary.copy(alpha = 0.5f)), cardShape)
                    .clickable {
                        scope.launch {
                            killer?.sweepNow()
                            stats = withContext(Dispatchers.IO) { SystemStatsReader.readSnapshot(context) }
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "⚡ كنس برامج الخلفية وتسريع الرام (Process Killer)",
                            color = tokens.primary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (killerState.isRunningSweep) {
                            Text(
                                text = "⏳ جارٍ التحرير...",
                                color = tokens.onSurfaceMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    val lastRes = killerState.lastResult
                    val killerStatus = if (lastRes != null) {
                        "تم إيقاف ${lastRes.killedCount} تطبيق في الخلفية • تحرير ${String.format(Locale.US, "%.1f", lastRes.freedMb)} MB رام"
                    } else {
                        "يقوم بإيقاف التطبيقات والواجهات الخارجية المعلقة في الخلفية لتحرير المعالج والرام"
                    }
                    Text(
                        text = killerStatus,
                        color = tokens.onSurfaceMuted,
                        fontSize = 13.sp,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "tools:toggle_auto_guard", radius = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (killerState.isAutoGuardEnabled) tokens.primary.copy(alpha = 0.25f) else tokens.surface)
                            .border(BorderStroke(1.dp, if (killerState.isAutoGuardEnabled) tokens.primary else tokens.stroke), RoundedCornerShape(10.dp))
                            .clickable { killer?.toggleAutoGuard(!killerState.isAutoGuardEnabled) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (killerState.isAutoGuardEnabled) "🛡 الحارس التلقائي: مفعّل" else "🛡 الحارس: معطّل",
                            color = if (killerState.isAutoGuardEnabled) tokens.primary else tokens.onSurfaceMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = "كنس الرام ❯",
                        color = tokens.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(cleanButtonFocus)
                    .mishkatFocus(id = "tools:clean_now")
                    .clip(cardShape)
                    .background(tokens.accentTools)
                    .border(BorderStroke(1.dp, tokens.primary), cardShape)
                    .clickable {
                        scope.launch {
                            lastCleanResult = HonestCleaner.cleanNow(context)
                            stats = withContext(Dispatchers.IO) { SystemStatsReader.readSnapshot(context) }
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.tools_clean_button),
                        color = tokens.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val res = lastCleanResult
                    val statusLine = if (res != null) {
                        stringResource(
                            R.string.tools_clean_result_fmt,
                            SystemStatsReader.formatBytes(res.freedBytes),
                            res.deletedFilesCount,
                        )
                    } else {
                        stringResource(
                            R.string.tools_cache_current_fmt,
                            SystemStatsReader.formatBytes(stats.appCacheBytes),
                        )
                    }
                    Text(
                        text = statusLine,
                        color = tokens.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = "OK",
                    color = tokens.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Honest Downloaded APK & Installer Residue Cleaner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .mishkatFocus(id = "tools:clean_apks")
                    .clip(cardShape)
                    .background(tokens.card)
                    .border(BorderStroke(1.dp, tokens.stroke), cardShape)
                    .clickable {
                        scope.launch {
                            lastApkCleanResult = HonestCleaner.cleanOldApks(context)
                            stats = withContext(Dispatchers.IO) { SystemStatsReader.readSnapshot(context) }
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🗑️ مسح ملفات الـ APK القديمة ومخلفات التحميل",
                        color = tokens.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val apkRes = lastApkCleanResult
                    val apkStatus = if (apkRes != null) {
                        if (apkRes.deletedFilesCount > 0) {
                            "تم حذف ${apkRes.deletedFilesCount} ملف APK وتحرير ${SystemStatsReader.formatBytes(apkRes.freedBytes)}"
                        } else {
                            "مجلد التحميلات نظيف تماماً، لا توجد ملفات APK متراكمة"
                        }
                    } else {
                        "يفحص مجلد Download ويحذف حزم التثبيت المؤقتة لتوفير مساحة حقيقية"
                    }
                    Text(
                        text = apkStatus,
                        color = if (apkRes != null && apkRes.freedBytes > 0) tokens.primary else tokens.onSurfaceMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Text(
                    text = "مسح ❯",
                    color = tokens.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Direct shortcut to Bloatware / Background App Manager
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .mishkatFocus(id = "tools:open_bloatware_mgr")
                    .clip(cardShape)
                    .background(tokens.card)
                    .border(BorderStroke(1.dp, tokens.stroke), cardShape)
                    .clickable { onOpenAppManager() }
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.app_drawer_title),
                        color = tokens.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.tools_bloatware_hint),
                        color = tokens.onSurfaceMuted,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    text = "→",
                    color = tokens.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MetricGaugeCard(
    modifier: Modifier,
    tokens: ThemeTokens,
    title: String,
    primaryValue: String,
    detail: String,
) {
    val shape = RoundedCornerShape(tokens.cardRadius)
    Column(
        modifier = modifier
            .height(132.dp)
            .clip(shape)
            .background(tokens.card)
            .border(BorderStroke(1.dp, tokens.stroke), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = tokens.onSurfaceMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = primaryValue,
            color = tokens.primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = detail,
            color = tokens.onSurface,
            fontSize = 12.sp,
        )
    }
}

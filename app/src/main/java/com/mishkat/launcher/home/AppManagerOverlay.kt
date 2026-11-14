package com.mishkat.launcher.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.AppSettings
import com.mishkat.launcher.theme.IconMask
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.WALLPAPER_PRESETS
import com.mishkat.launcher.theme.liquidGlass

/**
 * §10 S4: Full App Drawer + Bloatware/Ad-App Manager.
 * Lets the user:
 * - Filter by Visible / Pinned ★ / Hidden (Ad & Unused apps).
 * - Jump by first letter.
 * - Pin favorite apps to the start of the Home shelf.
 * - Hide unused/ad-heavy preinstalled apps from the Home screen.
 * - Open System App Details (`ACTION_APPLICATION_DETAILS_SETTINGS`) to Force Stop / Disable bloatware.
 * - Trigger package uninstall (`ACTION_DELETE`).
 */
@Composable
fun AppManagerOverlay(
    tokens: ThemeTokens,
    allApps: List<AppEntry>,
    onLaunch: (ComponentName) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleHide: (String) -> Unit,
    onMoveApp: (String, Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) } // 0=Visible, 1=Pinned, 2=Hidden
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    val firstFocus = remember { FocusRequester() }
    val iconShape = remember(tokens.iconMask) { IconMask.shapeFor(tokens.iconMask) }

    val visibleCount = remember(allApps) { allApps.count { !it.isHidden } }
    val pinnedCount = remember(allApps) { allApps.count { it.isPinned && !it.isHidden } }
    val hiddenCount = remember(allApps) { allApps.count { it.isHidden } }

    val letters = remember(allApps) {
        allApps.mapNotNull { it.label.trim().firstOrNull { ch -> ch.isLetter() } }
            .distinct()
            .take(18)
    }

    val filtered = remember(allApps, tab, selectedLetter) {
        val byTab = when (tab) {
            1 -> allApps.filter { it.isPinned && !it.isHidden }
            2 -> allApps.filter { it.isHidden }
            else -> allApps.filter { !it.isHidden }
        }
        val ch = selectedLetter
        if (ch == null) byTab else byTab.filter { it.label.trim().startsWith(ch, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        repeat(4) {
            withFrameNanos { }
            if (runCatching { firstFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.surface.copy(alpha = 0.96f))
            .padding(horizontal = 56.dp, vertical = 34.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header + Filter Tabs + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.app_drawer_title),
                        color = tokens.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    DrawerPill(
                        id = "drawer:tab_visible",
                        label = stringResource(R.string.app_drawer_tab_visible, visibleCount),
                        tokens = tokens,
                        highlighted = tab == 0,
                        modifier = Modifier.focusRequester(firstFocus),
                        onClick = {
                            tab = 0
                            selectedLetter = null
                        },
                    )

                    DrawerPill(
                        id = "drawer:tab_pinned",
                        label = stringResource(R.string.app_drawer_tab_pinned, pinnedCount),
                        tokens = tokens,
                        highlighted = tab == 1,
                        onClick = {
                            tab = 1
                            selectedLetter = null
                        },
                    )

                    DrawerPill(
                        id = "drawer:tab_hidden",
                        label = stringResource(R.string.app_drawer_tab_hidden, hiddenCount),
                        tokens = tokens,
                        highlighted = tab == 2,
                        onClick = {
                            tab = 2
                            selectedLetter = null
                        },
                    )
                }

                DrawerPill(
                    id = "drawer:close",
                    label = stringResource(R.string.radio_close),
                    tokens = tokens,
                    highlighted = false,
                    onClick = onDismiss,
                )
            }

            if (letters.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    items(items = letters, key = { it.code }) { ch ->
                        val active = selectedLetter == ch
                        DrawerPill(
                            id = "drawer:letter_$ch",
                            label = ch.toString(),
                            tokens = tokens,
                            highlighted = active,
                            onClick = { selectedLetter = if (active) null else ch },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.shelf_apps_empty),
                        color = tokens.onSurfaceMuted,
                        fontSize = 18.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(
                        items = filtered,
                        key = { it.key },
                        contentType = { "drawer_app_row" },
                    ) { app ->
                        val shape = RoundedCornerShape(tokens.cardRadius)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Main Launch Card
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .mishkatFocus(id = "drawer_open:${app.key}")
                                    .clip(shape)
                                    .background(tokens.card)
                                    .border(BorderStroke(1.dp, tokens.stroke), shape)
                                    .clickable { onLaunch(app.component) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(iconShape),
                                    )
                                } else {
                                    Spacer(Modifier.size(38.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = tokens.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = tokens.onSurfaceMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            // Move Up / Earlier
                            DrawerPill(
                                id = "drawer_up:${app.key}",
                                label = "▲",
                                tokens = tokens,
                                highlighted = false,
                                onClick = { onMoveApp(app.packageName, -1) },
                            )

                            // Move Down / Later
                            DrawerPill(
                                id = "drawer_down:${app.key}",
                                label = "▼",
                                tokens = tokens,
                                highlighted = false,
                                onClick = { onMoveApp(app.packageName, 1) },
                            )

                            // Pin / Unpin button
                            DrawerPill(
                                id = "drawer_pin:${app.key}",
                                label = if (app.isPinned) "★" else "☆",
                                tokens = tokens,
                                highlighted = app.isPinned,
                                onClick = { onTogglePin(app.packageName) },
                            )

                            // Hide / Unhide from Home button
                            DrawerPill(
                                id = "drawer_hide:${app.key}",
                                label = stringResource(
                                    if (app.isHidden) R.string.app_action_unhide else R.string.app_action_hide,
                                ),
                                tokens = tokens,
                                highlighted = app.isHidden,
                                onClick = { onToggleHide(app.packageName) },
                            )

                            // System App Info (Force Stop / Disable Bloatware / Clear Cache)
                            DrawerPill(
                                id = "drawer_info:${app.key}",
                                label = stringResource(R.string.app_action_force_stop_info),
                                tokens = tokens,
                                highlighted = false,
                                onClick = { openAppSystemDetails(context, app.packageName) },
                            )

                            // Uninstall
                            DrawerPill(
                                id = "drawer_uninstall:${app.key}",
                                label = stringResource(R.string.app_action_uninstall),
                                tokens = tokens,
                                highlighted = false,
                                onClick = { requestUninstall(context, app.packageName) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * §10 S4 + S6 + R21: Full Launcher Control Settings Panel:
 * - Apps shelf position (Top vs Bottom)
 * - Layout density / card scale (Compact vs Comfortable)
 * - Liquid Glass intensity (55% / 90% / 110%)
 * - Islamic Wallpaper / Art selector + gallery shortcut
 * - Lock Screen / Ambient Mode timeout + Lock Now trigger
 * - Instant Arabic/English switch without restart
 * - 12/24h time format toggle
 * - Launch on Boot toggle + Default Home chooser + Android TV System Settings
 */
enum class SettingsCategory(val id: String, val titleAr: String, val titleEn: String, val glyph: String) {
    APPEARANCE("appearance", "المظهر والسمات", "Appearance & Themes", "🎨"),
    SHELVES("shelves", "ترتيب أقسام الشاشة", "Home Shelves Order", "📱"),
    STREAMING("streaming", "البث وتوفير الباقة", "Streaming & Data Saver", "📺"),
    NOTIFICATIONS("notifications", "مركز الإشعارات والتحديثات", "Notifications & Updates", "🔔"),
    SYSTEM("system", "النظام ومطور التطبيق", "System & Developer", "⚙");
}

@Composable
fun SettingsOverlay(
    tokens: ThemeTokens,
    settings: AppSettings,
    onToggleLightMode: () -> Unit = {},
    onCycleFontScale: () -> Unit = {},
    onToggleAppsAtTop: () -> Unit,
    onToggleCompactLayout: () -> Unit,
    onCycleGlassIntensity: () -> Unit,
    onCycleWallpaperStyle: () -> Unit,
    onOpenWallpapersGallery: () -> Unit,
    onCycleLockTimeout: () -> Unit,
    onLockScreenNow: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggle24Hour: () -> Unit,
    onToggleLaunchOnBoot: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onOpenSafeModeLauncherChooser: () -> Unit,
    onToggleDataSaver: () -> Unit = {},
    onMoveShelfUp: (String) -> Unit = {},
    onMoveShelfDown: (String) -> Unit = {},
    onOpenDeveloperProfile: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    var activeCategory by remember { mutableStateOf(SettingsCategory.APPEARANCE) }
    val firstFocus = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        repeat(4) {
            withFrameNanos { }
            if (runCatching { firstFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    val activeWallpaper = remember(settings.wallpaperStyle, settings.isArabic) {
        val preset = WALLPAPER_PRESETS.firstOrNull { it.id == settings.wallpaperStyle } ?: WALLPAPER_PRESETS.first()
        if (settings.isArabic) preset.titleAr else preset.titleEn
    }

    val lockTimeoutLabel = when (settings.lockTimeoutMin) {
        0 -> stringResource(R.string.settings_disabled)
        else -> "${settings.lockTimeoutMin} min"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.surface.copy(alpha = 0.94f))
            .padding(horizontal = 48.dp, vertical = 26.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Title + Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "⚙", fontSize = 24.sp)
                    Column {
                        Text(
                            text = if (settings.isArabic) "إعدادات لانشر مشكاة الذكي" else "Mishkat Smart TV Settings",
                            color = tokens.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (settings.isArabic) "واجهة تحكم مقسمة على طراز Apple TV" else "Apple TV Style Split Interface",
                            color = tokens.onSurfaceMuted,
                            fontSize = 12.sp,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrawerPill(
                        id = "settings:lock_now",
                        label = stringResource(R.string.section_lock_screen),
                        tokens = tokens,
                        highlighted = true,
                        onClick = onLockScreenNow,
                    )
                    DrawerPill(
                        id = "settings:open_gallery",
                        label = stringResource(R.string.section_wallpapers),
                        tokens = tokens,
                        highlighted = false,
                        onClick = onOpenWallpapersGallery,
                    )
                    DrawerPill(
                        id = "settings:close",
                        label = stringResource(R.string.radio_close),
                        tokens = tokens,
                        highlighted = false,
                        onClick = onDismiss,
                    )
                }
            }

            // Apple TV Split Layout: Left Navigation Sidebar + Right Content Pane
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Left Sidebar (260dp)
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsCategory.entries.forEachIndexed { index, cat ->
                        val isSelected = cat == activeCategory
                        val tabFocusMod = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier
                        Row(
                            modifier = tabFocusMod
                                .fillMaxWidth()
                                .mishkatFocus(id = "settings:tab:${cat.id}", radius = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .liquidGlass(
                                    tokens = tokens,
                                    baseColor = if (isSelected) tokens.cardFocused else tokens.card,
                                    radius = 12.dp,
                                    glassIntensity = settings.glassIntensity,
                                    borderColor = if (isSelected) tokens.primary else tokens.glassRim,
                                )
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        activeCategory = cat
                                    }
                                }
                                .clickable { activeCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(text = cat.glyph, fontSize = 18.sp)
                            Text(
                                text = if (settings.isArabic) cat.titleAr else cat.titleEn,
                                color = if (isSelected) tokens.primary else tokens.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }

                // Right Pane: Active Category Details Cards
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                ) {
                    when (activeCategory) {
                        SettingsCategory.APPEARANCE -> {
                            item(key = "light_dark_mode") {
                                SettingRowCard(
                                    id = "settings:light_dark",
                                    title = if (settings.isArabic) "وضع الإضاءة والألوان (Light / Dark Mode)" else "Appearance Mode (Light / Dark)",
                                    value = if (settings.isLightMode) {
                                        if (settings.isArabic) "☀️ الوضع النهاري الهادئ" else "☀️ Light Mode"
                                    } else {
                                        if (settings.isArabic) "🌙 الوضع الليلي الداكن" else "🌙 Dark Mode"
                                    },
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onToggleLightMode,
                                )
                            }

                            item(key = "font_scale") {
                                val fontScaleLabel = when {
                                    settings.fontScale > 1.06f -> if (settings.isArabic) "كبير وواضح (115%)" else "Large & Clear (115%)"
                                    settings.fontScale < 0.96f -> if (settings.isArabic) "مدمج (90%)" else "Compact (90%)"
                                    else -> if (settings.isArabic) "قياسي متوازن (100%)" else "Standard (100%)"
                                }
                                SettingRowCard(
                                    id = "settings:font_scale",
                                    title = if (settings.isArabic) "حجم الخطوط والنصوص" else "Font & Text Size",
                                    value = fontScaleLabel,
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onCycleFontScale,
                                )
                            }

                            item(key = "glass_level") {
                                SettingRowCard(
                                    id = "settings:glass",
                                    title = stringResource(R.string.settings_glass_label),
                                    value = "${(settings.glassIntensity * 100).toInt()}%",
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onCycleGlassIntensity,
                                )
                            }

                            item(key = "wallpaper_style") {
                                SettingRowCard(
                                    id = "settings:wallpaper",
                                    title = stringResource(R.string.settings_wallpaper_label),
                                    value = activeWallpaper,
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onCycleWallpaperStyle,
                                )
                            }

                            item(key = "open_wallpapers_btn") {
                                SettingRowCard(
                                    id = "settings:open_gallery_card",
                                    title = if (settings.isArabic) "معرض الخلفيات والصور والزخارف الإسلامية" else "Full Wallpapers & Calligraphy Gallery",
                                    value = "🖼 عرض المعرض",
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onOpenWallpapersGallery,
                                )
                            }

                            item(key = "lang") {
                                SettingRowCard(
                                    id = "settings:lang",
                                    title = stringResource(R.string.settings_language_label),
                                    value = if (settings.isArabic) "العربية (RTL)" else "English (LTR)",
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onToggleLanguage,
                                )
                            }

                            item(key = "clock") {
                                SettingRowCard(
                                    id = "settings:clock",
                                    title = stringResource(R.string.settings_clock_format_label),
                                    value = stringResource(
                                        if (settings.use24Hour) R.string.settings_clock_24h else R.string.settings_clock_12h,
                                    ),
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onToggle24Hour,
                                )
                            }
                        }

                        SettingsCategory.SHELVES -> {
                            item(key = "shelf_ordering_header") {
                                Text(
                                    text = if (settings.isArabic) "✦ ترتيب صفوف الشاشة الرئيسية (تحريك للأعلى أو للأسفل):"
                                    else "✦ Home Shelves Order (Move Up / Down):",
                                    color = tokens.primary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                                )
                            }

                            itemsIndexed(settings.shelfOrder, key = { _, it -> "shelf_order_$it" }) { index, shelfId ->
                                val shelfTitle = when (shelfId) {
                                    "livetv" -> if (settings.isArabic) "📺 البث الحي للقنوات (مصر والعالم العربي)" else "📺 Live TV Channels"
                                    "apps" -> if (settings.isArabic) "📱 التطبيقات المثبتة والألعاب" else "📱 Installed Apps & Games"
                                    "prayer" -> if (settings.isArabic) "🕌 مواقيت الصلاة والأذكار والقبلة" else "🕌 Prayer Times & Adhkar"
                                    "sections" -> if (settings.isArabic) "✦ شريط أقسام اللانشر الرئيسي" else "✦ Main Sections Shelf"
                                    "radio" -> if (settings.isArabic) "📻 إذاعة القرآن الكريم والقراء" else "📻 Holy Quran Radio & Reciters"
                                    "tv" -> if (settings.isArabic) "📡 الريسيفر الفضائي ومداخل الشاشة" else "📡 TV Receiver & HDMI Inputs"
                                    "sports" -> if (settings.isArabic) "⚽ مركز المباريات والرياضة" else "⚽ Sports & Match Center"
                                    else -> shelfId
                                }
                                val shape = RoundedCornerShape(10.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(shape)
                                        .liquidGlass(tokens, tokens.card, 10.dp, settings.glassIntensity)
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(tokens.primary.copy(alpha = 0.25f))
                                                .border(1.dp, tokens.primary, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = tokens.primary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Text(
                                            text = shelfTitle,
                                            color = tokens.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (index > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .mishkatFocus(id = "shelf_up_$shelfId", radius = 6.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(tokens.cardFocused)
                                                    .clickable { onMoveShelfUp(shelfId) }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                            ) {
                                                Text(text = "▲ للأعلى", color = tokens.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (index < settings.shelfOrder.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .mishkatFocus(id = "shelf_down_$shelfId", radius = 6.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(tokens.cardFocused)
                                                    .clickable { onMoveShelfDown(shelfId) }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                            ) {
                                                Text(text = "▼ للأسفل", color = tokens.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "apps_pos") {
                                SettingRowCard(
                                    id = "settings:apps_pos",
                                    title = stringResource(R.string.settings_apps_position_label),
                                    value = stringResource(
                                        if (settings.appsAtTop) R.string.settings_apps_top else R.string.settings_apps_bottom,
                                    ),
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onToggleAppsAtTop,
                                )
                            }

                            item(key = "card_scale") {
                                SettingRowCard(
                                    id = "settings:card_scale",
                                    title = stringResource(R.string.settings_card_size_label),
                                    value = stringResource(
                                        if (settings.compactLayout) R.string.settings_card_compact else R.string.settings_card_comfortable,
                                    ),
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onToggleCompactLayout,
                                )
                            }
                        }

                        SettingsCategory.STREAMING -> {
                            item(key = "data_saver") {
                                SettingRowCard(
                                    id = "settings:data_saver",
                                    title = if (settings.isArabic) "نظام توفير باقة الإنترنت المنزلية (Data Saver)" else "Home Internet Data Saver",
                                    value = if (settings.dataSaverMode) (if (settings.isArabic) "🛡 مفعّل (توفير الباقة)" else "🛡 Enabled")
                                    else (if (settings.isArabic) "معطّل (جودة كاملة)" else "Disabled"),
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onToggleDataSaver,
                                )
                            }
                        }

                        SettingsCategory.NOTIFICATIONS -> {
                            item(key = "notif_permission_card") {
                                SettingRowCard(
                                    id = "settings:notif_permission",
                                    title = if (settings.isArabic) "إذن قراءة إشعارات التطبيقات (Notification Access)" else "App Notification Access Permission",
                                    value = "🔔 فتح إعدادات الإذن",
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = { com.mishkat.launcher.notification.NotificationRepository.openPermissionSettings(context) },
                                )
                            }

                            item(key = "notif_hub_card") {
                                SettingRowCard(
                                    id = "settings:notif_hub_open",
                                    title = if (settings.isArabic) "مركز الإشعارات وتحديثات النظام (OTA Updates)" else "Notifications & System Updates Hub",
                                    value = "عرض المركز 🔔",
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onOpenNotifications,
                                )
                            }
                        }

                        SettingsCategory.SYSTEM -> {
                            item(key = "dev_profile") {
                                SettingRowCard(
                                    id = "settings:developer_profile",
                                    title = if (settings.isArabic) "ملف مطور التطبيق" else "App Developer Profile",
                                    value = "Eng. Mohamed Okash 👨‍💻",
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onOpenDeveloperProfile,
                                )
                            }

                            item(key = "lock_timeout") {
                                SettingRowCard(
                                    id = "settings:lock_timeout",
                                    title = stringResource(R.string.settings_lock_timeout_label),
                                    value = lockTimeoutLabel,
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onCycleLockTimeout,
                                )
                            }

                            item(key = "boot") {
                                SettingRowCard(
                                    id = "settings:boot",
                                    title = stringResource(R.string.settings_boot_launch_label),
                                    value = stringResource(
                                        if (settings.launchOnBoot) R.string.settings_enabled else R.string.settings_disabled,
                                    ),
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onToggleLaunchOnBoot,
                                )
                            }

                            item(key = "default_home") {
                                SettingRowCard(
                                    id = "settings:default_home",
                                    title = stringResource(R.string.safe_mode_system_launcher),
                                    value = "HOME",
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onOpenSafeModeLauncherChooser,
                                )
                            }

                            item(key = "android_settings") {
                                SettingRowCard(
                                    id = "settings:system",
                                    title = stringResource(R.string.settings_open_android_tv),
                                    value = "⚙",
                                    tokens = tokens,
                                    glassIntensity = settings.glassIntensity,
                                    onClick = onOpenAndroidSettings,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRowCard(
    id: String,
    title: String,
    value: String,
    tokens: ThemeTokens,
    glassIntensity: Float = 0.85f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(tokens.cardRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .mishkatFocus(id = id)
            .clip(shape)
            .liquidGlass(tokens, tokens.card, tokens.cardRadius, glassIntensity)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = tokens.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            color = tokens.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DrawerPill(
    id: String,
    label: String,
    tokens: ThemeTokens,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .mishkatFocus(id = id, radius = 14.dp)
            .clip(shape)
            .background(if (highlighted) tokens.cardFocused else tokens.card)
            .border(BorderStroke(1.dp, if (highlighted) tokens.primary else tokens.stroke), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (highlighted) tokens.primary else tokens.onSurface,
            fontSize = 13.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

fun openAppSystemDetails(context: Context, packageName: String) {
    runCatching {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

fun requestUninstall(context: Context, packageName: String) {
    runCatching {
        val intent = Intent(
            Intent.ACTION_DELETE,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

package com.mishkat.launcher.home

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import com.mishkat.launcher.notification.NotificationRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
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
import com.mishkat.launcher.core.AppGraph
import com.mishkat.launcher.core.Logger
import com.mishkat.launcher.focus.LocalFocusRestorer
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.AppSettings
import com.mishkat.launcher.i18n.LocalAppSettings
import com.mishkat.launcher.i18n.LocaleFormatter
import com.mishkat.launcher.input.MishkatAction
import com.mishkat.launcher.input.RemoteUnificationLayer
import com.mishkat.launcher.prayer.AdhanOverlay
import com.mishkat.launcher.prayer.AdhkarPanelOverlay
import com.mishkat.launcher.prayer.CityPickerOverlay
import com.mishkat.launcher.prayer.PrayerAndAdhkarRow
import com.mishkat.launcher.prayer.PrayerUiState
import com.mishkat.launcher.prayer.adhkarWindowLabel
import com.mishkat.launcher.radio.NowPlayingBar
import com.mishkat.launcher.radio.RadioPanelOverlay
import com.mishkat.launcher.radio.RecitersQuickShelf
import com.mishkat.launcher.remote.CompanionCommand
import com.mishkat.launcher.remote.CompanionRemoteOverlay
import com.mishkat.launcher.sports.SportsPanelOverlay
import com.mishkat.launcher.sports.SportsQuickShelf
import com.mishkat.launcher.sports.SportsUiState
import com.mishkat.launcher.theme.IconMask
import com.mishkat.launcher.theme.LocalThemeTokens
import com.mishkat.launcher.theme.ThemePackManagerOverlay
import com.mishkat.launcher.theme.ThemeSpec
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.ThemeWallpaper
import com.mishkat.launcher.theme.WALLPAPER_PRESETS
import com.mishkat.launcher.theme.liquidGlass
import com.mishkat.launcher.tools.DeviceToolsOverlay
import com.mishkat.launcher.tv.TvInputsPanelOverlay
import com.mishkat.launcher.tv.TvInputsQuickShelf
import com.mishkat.launcher.tv.TvInputsUiState
import com.mishkat.launcher.weather.WeatherPanelOverlay
import com.mishkat.launcher.weather.WeatherSnapshot
import com.mishkat.launcher.weather.weatherCodeGlyph
import com.mishkat.launcher.livetv.ChannelCategory
import com.mishkat.launcher.livetv.LiveChannel
import com.mishkat.launcher.livetv.LiveTvPlayerOverlay
import com.mishkat.launcher.livetv.LiveTvQuickShelf
import com.mishkat.launcher.update.UpdateUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

private enum class ActiveOverlay {
    NONE,
    LIVE_TV,
    RADIO,
    SPORTS,
    TV_INPUTS,
    REMOTE_QR,
    CITY_PICKER,
    ADHKAR,
    WEATHER,
    THEMES_WALLPAPERS,
    TOOLS,
    APP_MANAGER,
    SETTINGS,
    LOCK_SCREEN,
    NOTIFICATIONS,
    DEVELOPER_PROFILE,
}

@Immutable
private data class SectionCardSpec(
    val id: String,
    val titleRes: Int,
    val glyph: String,
)

private val SECTION_CARDS = listOf(
    SectionCardSpec("sec:live_tv", R.string.section_live_tv, "📺"),
    SectionCardSpec("sec:notifications", R.string.section_notifications, "🔔"),
    SectionCardSpec("sec:developer", R.string.section_developer, "👨‍💻"),
    SectionCardSpec("sec:tv_inputs", R.string.section_tv_inputs, "📡"),
    SectionCardSpec("sec:radio", R.string.section_radio, "📻"),
    SectionCardSpec("sec:sports", R.string.section_sports, "⚽"),
    SectionCardSpec("sec:wallpapers", R.string.section_wallpapers, "🖼"),
    SectionCardSpec("sec:themes", R.string.section_themes, "✦"),
    SectionCardSpec("sec:remote_qr", R.string.section_remote_qr, "📱"),
    SectionCardSpec("sec:prayer", R.string.section_prayer, "🕌"),
    SectionCardSpec("sec:adhkar", R.string.section_adhkar, "📿"),
    SectionCardSpec("sec:weather", R.string.section_weather, "🌦"),
    SectionCardSpec("sec:tools", R.string.section_tools, "🧹"),
    SectionCardSpec("sec:lock", R.string.section_lock_screen, "🔒"),
    SectionCardSpec("sec:settings", R.string.section_settings, "⚙"),
)

@Composable
fun HomeScreen(
    graph: AppGraph,
    onOpenHomeChooser: () -> Unit = {},
) {
    val tokens = LocalThemeTokens.current
    val appSettings = LocalAppSettings.current
    val themes by graph.themeRepository.themes.collectAsState()
    val online by graph.network.online.collectAsState()
    val appsState by graph.appsRepository.state.collectAsState()
    val stations by graph.stationRepository.stations.collectAsState()
    val radioState by graph.radioController.state.collectAsState()
    val sleepState by graph.sleepTimerRepository.state.collectAsState()
    val prayerState by graph.prayerRepository.state.collectAsState()
    val cities by graph.prayerRepository.cities.collectAsState()
    val weatherSnapshot by graph.weatherRepository.snapshot.collectAsState()
    val sportsState by graph.sportsRepository.state.collectAsState()
    val tvInputsState by graph.tvInputsRepository.state.collectAsState()
    val liveTvState by graph.liveTvRepository.state.collectAsState()
    val remoteServerState by graph.companionRemoteServer.state.collectAsState()
    val updateState by graph.updateRepository.state.collectAsState()
    val appNotifications by NotificationRepository.notifications.collectAsState()

    val context = LocalContext.current
    val isArabic = appSettings.isArabic
    val compact = appSettings.compactLayout
    val restorer = LocalFocusRestorer.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Dedicated FocusRequesters for top-level entries
    val topBarFocus = remember { FocusRequester() }
    val npFocus = remember { FocusRequester() }
    val appsFocus = remember { FocusRequester() }
    val liveTvFocus = remember { FocusRequester() }
    val prayerFocus = remember { FocusRequester() }
    val sectionsFocus = remember { FocusRequester() }
    val recitersFocus = remember { FocusRequester() }
    val tvInputsFocus = remember { FocusRequester() }
    val sportsFocus = remember { FocusRequester() }
    val billboardFocus = remember { FocusRequester() }

    val shelfFocusMap = remember {
        mapOf(
            "livetv" to liveTvFocus,
            "apps" to appsFocus,
            "prayer" to prayerFocus,
            "sections" to sectionsFocus,
            "radio" to recitersFocus,
            "tv" to tvInputsFocus,
            "sports" to sportsFocus,
        )
    }

    val orderedShelves = remember(appSettings.shelfOrder, stations.isNotEmpty()) {
        appSettings.shelfOrder.filter { id ->
            if (id == "radio") stations.isNotEmpty() else true
        }
    }

    val firstCardFocus = orderedShelves.firstOrNull()?.let { shelfFocusMap[it] } ?: liveTvFocus

    // Single mutually-exclusive overlay state so two overlays can NEVER stack on top of each other!
    var activeOverlay by remember { mutableStateOf(ActiveOverlay.NONE) }
    var returnToNowPlaying by remember { mutableStateOf(false) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val anyOverlayOpen = activeOverlay != ActiveOverlay.NONE || prayerState.activeAdhanAlert != null

    // When overlay closes, smoothly restore focus back to the Home Screen after 1 frame
    var wasOverlayOpen by remember { mutableStateOf(false) }
    LaunchedEffect(anyOverlayOpen) {
        if (anyOverlayOpen) {
            wasOverlayOpen = true
        } else if (wasOverlayOpen) {
            wasOverlayOpen = false
            val targetRequester = if (returnToNowPlaying) npFocus else firstCardFocus
            returnToNowPlaying = false
            repeat(3) {
                withFrameNanos { }
                if (runCatching { targetRequester.requestFocus() }.isSuccess) return@LaunchedEffect
            }
        }
    }

    // Remote shortcuts + S8 TV/Receiver/Color keys + interaction tracking for Lock Screen timer.
    LaunchedEffect(Unit) {
        RemoteUnificationLayer.actions.collect { action ->
            lastInteractionMs = System.currentTimeMillis()
            when (action) {
                MishkatAction.Home -> {
                    activeOverlay = ActiveOverlay.NONE
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0)
                        runCatching { firstCardFocus.requestFocus() }
                    }
                }
                MishkatAction.Settings, MishkatAction.Menu -> activeOverlay = ActiveOverlay.SETTINGS
                MishkatAction.Search -> activeOverlay = ActiveOverlay.APP_MANAGER
                MishkatAction.TvLive -> activeOverlay = ActiveOverlay.LIVE_TV
                MishkatAction.TvInput -> activeOverlay = ActiveOverlay.TV_INPUTS
                MishkatAction.ChannelUp -> graph.radioController.next()
                MishkatAction.ChannelDown -> graph.radioController.previous()
                MishkatAction.ColorRed -> activeOverlay = ActiveOverlay.RADIO
                MishkatAction.ColorGreen -> activeOverlay = ActiveOverlay.TV_INPUTS
                MishkatAction.ColorYellow -> activeOverlay = ActiveOverlay.SPORTS
                MishkatAction.ColorBlue -> activeOverlay = ActiveOverlay.THEMES_WALLPAPERS
                else -> Unit
            }
        }
    }

    // S9: Mobile Companion Remote Wi-Fi Commands collector
    LaunchedEffect(Unit) {
        graph.companionRemoteServer.commands.collect { cmd ->
            lastInteractionMs = System.currentTimeMillis()
            when (cmd) {
                is CompanionCommand.SelectTheme -> graph.themeRepository.select(cmd.themeId)
                is CompanionCommand.SelectWallpaper -> graph.localeRepository.setWallpaperStyle(cmd.wallpaperId)
                is CompanionCommand.PlayStationIndex -> {
                    stations.getOrNull(cmd.index)?.let { graph.radioController.playStation(it) }
                }
                CompanionCommand.ToggleLockScreen -> {
                    activeOverlay = if (activeOverlay == ActiveOverlay.LOCK_SCREEN) {
                        ActiveOverlay.NONE
                    } else {
                        ActiveOverlay.LOCK_SCREEN
                    }
                }
                CompanionCommand.OpenTvReceiver -> activeOverlay = ActiveOverlay.TV_INPUTS
                CompanionCommand.OpenSportsCenter -> activeOverlay = ActiveOverlay.SPORTS
                is CompanionCommand.Key -> Unit
            }
        }
    }

    // Automatic Lock Screen / Islamic Ambient Mode after configured inactivity timeout.
    LaunchedEffect(appSettings.lockTimeoutMin, activeOverlay) {
        val timeoutMin = appSettings.lockTimeoutMin
        if (timeoutMin <= 0 || activeOverlay == ActiveOverlay.LOCK_SCREEN) return@LaunchedEffect
        val timeoutMs = timeoutMin * 60_000L
        while (true) {
            delay(15_000L)
            if (System.currentTimeMillis() - lastInteractionMs >= timeoutMs) {
                activeOverlay = ActiveOverlay.LOCK_SCREEN
                break
            }
        }
    }

    // Default initial focus when FocusRestorer has nothing to restore or target no longer exists.
    LaunchedEffect(Unit) {
        val target = restorer?.restoreTarget?.await()
        if (target == null) {
            repeat(3) {
                withFrameNanos { }
                if (runCatching { firstCardFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            }
        } else {
            delay(120)
            if (restorer.isSettled.not()) {
                restorer.abandon()
                runCatching { firstCardFocus.requestFocus() }
            }
        }
    }

    val hijriLabel = if (isArabic) prayerState.hijriDateAr else prayerState.hijriDateEn

    Box(modifier = Modifier.fillMaxSize()) {
        ThemeWallpaper(
            tokens = tokens,
            wallpaperOverride = appSettings.wallpaperStyle,
            weatherCode = weatherSnapshot?.currentWeatherCode,
            animate = !anyOverlayOpen,
        )

        // Critical Focus Isolation: when any overlay is open, the background HomeScreen Column
        // has canFocus = false so D-Pad navigation NEVER moves cards in the background!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = !anyOverlayOpen }
                .padding(
                    horizontal = if (compact) 32.dp else 44.dp,
                    vertical = if (compact) 12.dp else 18.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            // Row 0: TopBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { down = npFocus }
            ) {
                TopBar(
                    tokens = tokens,
                    themes = themes,
                    online = online,
                    hijriDate = hijriLabel,
                    weatherSnapshot = weatherSnapshot,
                    appSettings = appSettings,
                    unreadNotificationsCount = (if (updateState.hasUnreadNotification) 1 else 0) + appNotifications.size,
                    firstFocusRequester = topBarFocus,
                    onSelectTheme = { graph.themeRepository.select(it) },
                    onToggleLightMode = { graph.localeRepository.toggleLightMode() },
                    onToggleLanguage = { graph.localeRepository.toggleLanguage() },
                    onOpenWallpapers = { activeOverlay = ActiveOverlay.THEMES_WALLPAPERS },
                    onOpenTvInputs = { activeOverlay = ActiveOverlay.TV_INPUTS },
                    onOpenRemoteQr = { activeOverlay = ActiveOverlay.REMOTE_QR },
                    onOpenNotifications = { activeOverlay = ActiveOverlay.NOTIFICATIONS },
                    onOpenDeveloperProfile = { activeOverlay = ActiveOverlay.DEVELOPER_PROFILE },
                    onOpenSettings = { activeOverlay = ActiveOverlay.SETTINGS },
                    onLockScreen = { activeOverlay = ActiveOverlay.LOCK_SCREEN },
                    onOpenWeather = { activeOverlay = ActiveOverlay.WEATHER },
                )
            }

            // Row 1: Persistent NowPlayingBar + SleepTimer integration (§9 / R13)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties {
                        up = topBarFocus
                        down = firstCardFocus
                    }
            ) {
                NowPlayingBar(
                    tokens = tokens,
                    radioState = radioState,
                    sleepState = sleepState,
                    onTogglePlayPause = { graph.radioController.togglePlayPause() },
                    onNext = { graph.radioController.next() },
                    onPrev = { graph.radioController.previous() },
                    onToggleFavorite = {
                        radioState.currentStation?.let { graph.stationRepository.toggleFavorite(it.id) }
                    },
                    onCycleSleepTimer = { graph.sleepTimerRepository.cyclePreset() },
                    onOpenRadioPanel = {
                        returnToNowPlaying = true
                        activeOverlay = ActiveOverlay.RADIO
                    },
                    panelTriggerFocus = npFocus,
                )
            }

            // Scrollable Shelves Container: all shelves stay composed so D-Pad Up/Down is 100% instant & deterministic
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
            ) {
                orderedShelves.forEachIndexed { index, shelfId ->
                    val isFirstShelf = index == 0
                    val shelfModifier = if (isFirstShelf) Modifier.focusProperties { up = npFocus } else Modifier

                    when (shelfId) {
                        "livetv" -> LiveTvQuickShelf(
                            tokens = tokens,
                            state = liveTvState,
                            compact = compact,
                            glassIntensity = appSettings.glassIntensity,
                            isArabic = isArabic,
                            firstFocusRequester = liveTvFocus,
                            modifier = shelfModifier,
                            onSelectChannel = { channel ->
                                graph.liveTvRepository.playChannel(channel)
                                activeOverlay = ActiveOverlay.LIVE_TV
                            },
                            onOpenCategory = { cat ->
                                graph.liveTvRepository.selectCategory(cat)
                            },
                        )
                        "apps" -> AppsShelf(
                            tokens = tokens,
                            state = appsState,
                            compact = compact,
                            glassIntensity = appSettings.glassIntensity,
                            isArabic = isArabic,
                            firstCardFocus = appsFocus,
                            modifier = shelfModifier,
                            onLaunch = { launchApp(context, it) },
                            onTogglePin = { graph.appsRepository.togglePin(it) },
                            onToggleHide = { graph.appsRepository.toggleHide(it) },
                            onMoveApp = { pkg, delta -> graph.appsRepository.moveApp(pkg, delta) },
                            onOpenDrawer = { activeOverlay = ActiveOverlay.APP_MANAGER },
                            onRetry = { graph.appsRepository.reload() },
                        )
                        "prayer" -> PrayerAndAdhkarRow(
                            tokens = tokens,
                            prayerState = prayerState,
                            use24Hour = appSettings.use24Hour,
                            firstFocusRequester = prayerFocus,
                            onOpenCityPicker = { activeOverlay = ActiveOverlay.CITY_PICKER },
                            onIncrementTasbih = { graph.prayerRepository.incrementTasbih() },
                            onOpenAdhkarPanel = { activeOverlay = ActiveOverlay.ADHKAR },
                        )
                        "sections" -> SectionsShelf(
                            tokens = tokens,
                            stationsCount = stations.size,
                            prayerState = prayerState,
                            weatherSnapshot = weatherSnapshot,
                            sportsState = sportsState,
                            tvState = tvInputsState,
                            remoteUrl = remoteServerState.url,
                            appSettings = appSettings,
                            firstCardFocus = sectionsFocus,
                            onOpenLiveTv = { activeOverlay = ActiveOverlay.LIVE_TV },
                            onOpenTvInputsPanel = { activeOverlay = ActiveOverlay.TV_INPUTS },
                            onOpenRadioPanel = {
                                returnToNowPlaying = false
                                activeOverlay = ActiveOverlay.RADIO
                            },
                            onOpenSportsPanel = { activeOverlay = ActiveOverlay.SPORTS },
                            onOpenRemoteQrPanel = { activeOverlay = ActiveOverlay.REMOTE_QR },
                            onOpenCityPicker = { activeOverlay = ActiveOverlay.CITY_PICKER },
                            onOpenAdhkarPanel = { activeOverlay = ActiveOverlay.ADHKAR },
                            onOpenWeatherPanel = { activeOverlay = ActiveOverlay.WEATHER },
                            onOpenThemeManager = { activeOverlay = ActiveOverlay.THEMES_WALLPAPERS },
                            onOpenToolsPanel = { activeOverlay = ActiveOverlay.TOOLS },
                            onLockScreen = { activeOverlay = ActiveOverlay.LOCK_SCREEN },
                            onOpenSettingsPanel = { activeOverlay = ActiveOverlay.SETTINGS },
                            onOpenNotifications = { activeOverlay = ActiveOverlay.NOTIFICATIONS },
                            onOpenDeveloperProfile = { activeOverlay = ActiveOverlay.DEVELOPER_PROFILE },
                        )
                        "radio" -> if (stations.isNotEmpty()) {
                            RecitersQuickShelf(
                                tokens = tokens,
                                stations = stations,
                                currentStationId = radioState.currentStation?.id,
                                isPlaying = radioState.isPlaying,
                                firstFocusRequester = recitersFocus,
                                onSelectStation = { graph.radioController.playStation(it) },
                                onOpenAll = {
                                    returnToNowPlaying = false
                                    activeOverlay = ActiveOverlay.RADIO
                                },
                            )
                        }
                        "tv" -> TvInputsQuickShelf(
                            tokens = tokens,
                            tvState = tvInputsState,
                            firstFocusRequester = tvInputsFocus,
                            onSelectSource = { graph.tvInputsRepository.openSource(it) },
                            onOpenTvHub = { activeOverlay = ActiveOverlay.TV_INPUTS },
                        )
                        "sports" -> SportsQuickShelf(
                            tokens = tokens,
                            sportsState = sportsState,
                            firstFocusRequester = sportsFocus,
                            onOpenSportsCenter = { activeOverlay = ActiveOverlay.SPORTS },
                            onCycleFavoriteTeam = { graph.sportsRepository.cycleFavoriteTeam() },
                        )
                    }
                }

                // Compact Hero Billboard Carousel (S3 + R23)
                BillboardCarousel(
                    tokens = tokens,
                    isVisible = !anyOverlayOpen,
                    isArabic = isArabic,
                    radioState = radioState,
                    prayerState = prayerState,
                    weatherSnapshot = weatherSnapshot,
                    sportsState = sportsState,
                    firstFocusRequester = billboardFocus,
                    onActionSlide = { slide ->
                        when (slide) {
                            0 -> {
                                returnToNowPlaying = false
                                activeOverlay = ActiveOverlay.RADIO
                            }
                            1 -> activeOverlay = ActiveOverlay.ADHKAR
                            2 -> activeOverlay = ActiveOverlay.WEATHER
                            else -> activeOverlay = ActiveOverlay.SPORTS
                        }
                    },
                )
            }
        }

        // Single mutually-exclusive overlay renderer (prevents any overlay stacking!)
        when (activeOverlay) {
            ActiveOverlay.NONE -> Unit
            ActiveOverlay.LIVE_TV -> LiveTvPlayerOverlay(
                tokens = tokens,
                repository = graph.liveTvRepository,
                isArabic = isArabic,
                onClose = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.RADIO -> RadioPanelOverlay(
                tokens = tokens,
                stations = stations,
                radioState = radioState,
                sleepState = sleepState,
                onPlayStation = { graph.radioController.playStation(it) },
                onToggleFavorite = { graph.stationRepository.toggleFavorite(it) },
                onCycleSleepTimer = { graph.sleepTimerRepository.cyclePreset() },
                onRefreshOnline = { graph.stationRepository.refreshOnline() },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.SPORTS -> SportsPanelOverlay(
                tokens = tokens,
                sportsState = sportsState,
                onSelectFavoriteTeam = { graph.sportsRepository.selectFavoriteTeam(it) },
                onRefresh = { graph.sportsRepository.refreshOnline() },
                onApplySportsTheme = { graph.themeRepository.select("sports_arena") },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.TV_INPUTS -> TvInputsPanelOverlay(
                tokens = tokens,
                tvState = tvInputsState,
                onSelectSource = {
                    activeOverlay = ActiveOverlay.NONE
                    graph.tvInputsRepository.openSource(it)
                },
                onSetDefaultSource = { graph.tvInputsRepository.setDefaultSource(it) },
                onToggleAutoBootLiveTv = { graph.tvInputsRepository.setAutoStartLiveTvOnBoot(it) },
                onRefreshInputs = { graph.tvInputsRepository.refreshInputs() },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.REMOTE_QR -> CompanionRemoteOverlay(
                tokens = tokens,
                serverState = remoteServerState,
                onRefreshIp = { graph.companionRemoteServer.refreshIp() },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.CITY_PICKER -> CityPickerOverlay(
                tokens = tokens,
                cities = cities,
                selectedCityId = prayerState.city.id,
                onSelectCity = { graph.prayerRepository.selectCity(it) },
                onPreviewAdhan = {
                    activeOverlay = ActiveOverlay.NONE
                    graph.prayerRepository.triggerAdhanPreview()
                },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.ADHKAR -> AdhkarPanelOverlay(
                tokens = tokens,
                initialWindow = prayerState.activeWindow,
                onLoadWindow = { graph.prayerRepository.adhkarForWindow(it) },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.WEATHER -> WeatherPanelOverlay(
                tokens = tokens,
                cityName = prayerState.city.displayName(isArabic),
                snapshot = weatherSnapshot,
                onRefresh = { graph.weatherRepository.refreshNow() },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.THEMES_WALLPAPERS -> ThemePackManagerOverlay(
                tokens = tokens,
                themes = themes,
                selectedWallpaperId = appSettings.wallpaperStyle,
                isArabic = isArabic,
                glassIntensity = appSettings.glassIntensity,
                isLightMode = appSettings.isLightMode,
                onSelectTheme = { graph.themeRepository.select(it) },
                onSelectWallpaper = { graph.localeRepository.setWallpaperStyle(it) },
                onCycleGlassIntensity = { graph.localeRepository.cycleGlassIntensity() },
                onToggleLightMode = { graph.localeRepository.toggleLightMode() },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.TOOLS -> DeviceToolsOverlay(
                tokens = tokens,
                onOpenAppManager = { activeOverlay = ActiveOverlay.APP_MANAGER },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.APP_MANAGER -> {
                val allApps = (appsState as? AppsState.Ready)?.allApps.orEmpty()
                AppManagerOverlay(
                    tokens = tokens,
                    allApps = allApps,
                    onLaunch = { launchApp(context, it) },
                    onTogglePin = { graph.appsRepository.togglePin(it) },
                    onToggleHide = { graph.appsRepository.toggleHide(it) },
                    onMoveApp = { pkg, delta -> graph.appsRepository.moveApp(pkg, delta) },
                    onDismiss = { activeOverlay = ActiveOverlay.NONE },
                )
            }
            ActiveOverlay.SETTINGS -> SettingsOverlay(
                tokens = tokens,
                settings = appSettings,
                onToggleLightMode = { graph.localeRepository.toggleLightMode() },
                onCycleFontScale = { graph.localeRepository.cycleFontScale() },
                onToggleAppsAtTop = { graph.localeRepository.toggleAppsAtTop() },
                onToggleCompactLayout = { graph.localeRepository.toggleCompactLayout() },
                onCycleGlassIntensity = { graph.localeRepository.cycleGlassIntensity() },
                onCycleWallpaperStyle = { graph.localeRepository.cycleWallpaperStyle() },
                onOpenWallpapersGallery = { activeOverlay = ActiveOverlay.THEMES_WALLPAPERS },
                onCycleLockTimeout = { graph.localeRepository.cycleLockTimeout() },
                onLockScreenNow = { activeOverlay = ActiveOverlay.LOCK_SCREEN },
                onToggleLanguage = { graph.localeRepository.toggleLanguage() },
                onToggle24Hour = { graph.localeRepository.toggle24Hour() },
                onToggleLaunchOnBoot = { graph.localeRepository.toggleLaunchOnBoot() },
                onOpenAndroidSettings = { openSystemSettings(context) },
                onOpenSafeModeLauncherChooser = onOpenHomeChooser,
                onToggleDataSaver = { graph.localeRepository.toggleDataSaver() },
                onMoveShelfUp = { graph.localeRepository.moveShelfUp(it) },
                onMoveShelfDown = { graph.localeRepository.moveShelfDown(it) },
                onOpenDeveloperProfile = { activeOverlay = ActiveOverlay.DEVELOPER_PROFILE },
                onOpenNotifications = { activeOverlay = ActiveOverlay.NOTIFICATIONS },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.NOTIFICATIONS -> NotificationsOverlay(
                tokens = tokens,
                appSettings = appSettings,
                updateState = updateState,
                updateRepository = graph.updateRepository,
                onToggleDataSaver = { graph.localeRepository.toggleDataSaver() },
                onOpenDeveloperProfile = { activeOverlay = ActiveOverlay.DEVELOPER_PROFILE },
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.DEVELOPER_PROFILE -> DeveloperProfileOverlay(
                tokens = tokens,
                appSettings = appSettings,
                onDismiss = { activeOverlay = ActiveOverlay.NONE },
            )
            ActiveOverlay.LOCK_SCREEN -> LockScreenOverlay(
                tokens = tokens,
                appSettings = appSettings,
                prayerState = prayerState,
                radioState = radioState,
                weatherSnapshot = weatherSnapshot,
                onToggleRadioPlayPause = { graph.radioController.togglePlayPause() },
                onNextStation = { graph.radioController.next() },
                onCycleWallpaper = { graph.localeRepository.cycleWallpaperStyle() },
                onUnlock = {
                    lastInteractionMs = System.currentTimeMillis()
                    activeOverlay = ActiveOverlay.NONE
                },
            )
        }

        // §4 + §9 + §10 DoD: AdhanOverlay renders on top of any active screen/panel.
        val adhanAlert = prayerState.activeAdhanAlert
        if (adhanAlert != null) {
            AdhanOverlay(
                tokens = tokens,
                kind = adhanAlert,
                cityName = prayerState.city.displayName(isArabic),
                onDismiss = { graph.prayerRepository.dismissAdhanAlert() },
            )
        }
    }
}

@Composable
private fun TopBar(
    tokens: ThemeTokens,
    themes: List<ThemeSpec>,
    online: Boolean,
    hijriDate: String,
    weatherSnapshot: WeatherSnapshot?,
    appSettings: AppSettings,
    unreadNotificationsCount: Int = 0,
    firstFocusRequester: FocusRequester,
    onSelectTheme: (String) -> Unit,
    onToggleLightMode: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenWallpapers: () -> Unit,
    onOpenTvInputs: () -> Unit,
    onOpenRemoteQr: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenDeveloperProfile: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onLockScreen: () -> Unit,
    onOpenWeather: () -> Unit,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val glass = appSettings.glassIntensity

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                nowMillis = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val timeText = remember(nowMillis, appSettings.use24Hour, appSettings.language) {
        val pattern = if (appSettings.use24Hour) "HH:mm" else "hh:mm a"
        val raw = SimpleDateFormat(pattern, appSettings.locale).format(Date(nowMillis))
        LocaleFormatter.localizeDigits(raw, appSettings.isArabic)
    }
    val statusText = stringResource(if (online) R.string.status_online else R.string.status_offline)
    val statusCd = stringResource(R.string.cd_network_status, statusText)

    val isArabic = appSettings.isArabic
    val pillShape = RoundedCornerShape(10.dp)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Extreme Left Group: Always starts from the left edge (x=0)
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Developer Profile: Absolute extreme left!
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:dev_profile", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = tokens.cardFocused,
                            radius = 10.dp,
                            glassIntensity = glass,
                            borderColor = tokens.primary.copy(alpha = 0.45f),
                        )
                        .clickable { onOpenDeveloperProfile() }
                        .padding(horizontal = 11.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (isArabic) "👨‍💻 المطور" else "👨‍💻 Developer",
                        color = tokens.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }

                // Notifications Hub with unread badge
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:notifications", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = if (unreadNotificationsCount > 0) tokens.primary.copy(alpha = 0.24f) else tokens.card,
                            radius = 10.dp,
                            glassIntensity = glass,
                            borderColor = if (unreadNotificationsCount > 0) tokens.primary else tokens.glassRim,
                        )
                        .clickable { onOpenNotifications() }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = "🔔", fontSize = 11.sp)
                        if (unreadNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935)),
                            )
                        }
                    }
                }

                // Clock
                Text(
                    text = timeText,
                    color = tokens.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )

                // Weather
                if (weatherSnapshot != null) {
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "topbar:weather", radius = 10.dp)
                            .clip(pillShape)
                            .liquidGlass(tokens, tokens.card, 10.dp, glass)
                            .clickable { onOpenWeather() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "${weatherCodeGlyph(weatherSnapshot.currentWeatherCode)} ${weatherSnapshot.currentTempC}°C",
                            color = tokens.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Language
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:lang", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(tokens, tokens.card, 10.dp, glass)
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (isArabic) "EN" else "عربي",
                        color = tokens.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Extreme Right Group: Always aligned to the right edge
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // TV Inputs (📡) & Remote (📱)
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:tv_inputs", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(tokens, tokens.card, 10.dp, glass)
                        .clickable { onOpenTvInputs() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(text = "📡", color = tokens.primary, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:remote_qr", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(tokens, tokens.card, 10.dp, glass)
                        .clickable { onOpenRemoteQr() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(text = "📱", color = tokens.primary, fontSize = 11.sp)
                }

                // Wallpapers
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:wallpapers", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(tokens, tokens.card, 10.dp, glass)
                        .clickable { onOpenWallpapers() }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.section_wallpapers),
                        color = tokens.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Light / Dark Mode
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:light_dark", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(tokens, tokens.card, 10.dp, glass)
                        .clickable { onToggleLightMode() }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (appSettings.isLightMode) "☀️ فاتح" else "🌙 داكن",
                        color = tokens.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Theme quick cycle
                val activeSpec = themes.firstOrNull { it.id == tokens.id }
                val activeThemeName = if (isArabic) (activeSpec?.name ?: tokens.id) else (activeSpec?.nameEn ?: activeSpec?.name ?: tokens.id)
                Box(
                    modifier = Modifier
                        .focusRequester(firstFocusRequester)
                        .mishkatFocus(id = "topbar:theme_cycle", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(tokens, tokens.card, 10.dp, glass)
                        .clickable {
                            if (themes.isNotEmpty()) {
                                val idx = themes.indexOfFirst { it.id == tokens.id }
                                val next = themes[(idx + 1) % themes.size]
                                onSelectTheme(next.id)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "✦ $activeThemeName",
                        color = tokens.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Settings: Prominent Apple TV Button at the Top Right
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:settings", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = tokens.cardFocused,
                            radius = 10.dp,
                            glassIntensity = glass,
                            borderColor = tokens.primary,
                        )
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 11.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = "⚙", color = tokens.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isArabic) "الضبط" else "Settings",
                            color = tokens.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // App Name
                Text(
                    text = stringResource(R.string.app_name),
                    color = tokens.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SectionsShelf(
    tokens: ThemeTokens,
    stationsCount: Int,
    prayerState: PrayerUiState,
    weatherSnapshot: WeatherSnapshot?,
    sportsState: SportsUiState,
    tvState: TvInputsUiState,
    remoteUrl: String,
    appSettings: AppSettings,
    firstCardFocus: FocusRequester?,
    onOpenLiveTv: () -> Unit,
    onOpenTvInputsPanel: () -> Unit,
    onOpenRadioPanel: () -> Unit,
    onOpenSportsPanel: () -> Unit,
    onOpenRemoteQrPanel: () -> Unit,
    onOpenCityPicker: () -> Unit,
    onOpenAdhkarPanel: () -> Unit,
    onOpenWeatherPanel: () -> Unit,
    onOpenThemeManager: () -> Unit,
    onOpenToolsPanel: () -> Unit,
    onLockScreen: () -> Unit,
    onOpenSettingsPanel: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenDeveloperProfile: () -> Unit = {},
) {
    val isArabic = appSettings.isArabic
    val compact = appSettings.compactLayout
    val glass = appSettings.glassIntensity
    val activeWallpaperTitle = remember(appSettings.wallpaperStyle, isArabic) {
        val p = WALLPAPER_PRESETS.firstOrNull { it.id == appSettings.wallpaperStyle } ?: WALLPAPER_PRESETS.first()
        if (isArabic) p.titleAr else p.titleEn
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.shelf_sections),
            color = tokens.onSurface,
            fontSize = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SECTION_CARDS.forEachIndexed { index, item ->
                val shape = RoundedCornerShape(tokens.cardRadius)
                val baseModifier = if (firstCardFocus != null && index == 0) {
                    Modifier.focusRequester(firstCardFocus)
                } else {
                    Modifier
                }

                Box(
                    modifier = baseModifier
                        .width(if (compact) 166.dp else 192.dp)
                        .height(if (compact) 70.dp else 84.dp)
                        .mishkatFocus(id = item.id)
                        .clip(shape)
                        .liquidGlass(tokens, tokens.card, tokens.cardRadius, glass)
                        .clickable {
                            when (item.id) {
                                "sec:live_tv" -> onOpenLiveTv()
                                "sec:notifications" -> onOpenNotifications()
                                "sec:developer" -> onOpenDeveloperProfile()
                                "sec:tv_inputs" -> onOpenTvInputsPanel()
                                "sec:radio" -> onOpenRadioPanel()
                                "sec:sports" -> onOpenSportsPanel()
                                "sec:wallpapers" -> onOpenThemeManager()
                                "sec:themes" -> onOpenThemeManager()
                                "sec:remote_qr" -> onOpenRemoteQrPanel()
                                "sec:prayer" -> onOpenCityPicker()
                                "sec:adhkar" -> onOpenAdhkarPanel()
                                "sec:weather" -> onOpenWeatherPanel()
                                "sec:tools" -> onOpenToolsPanel()
                                "sec:lock" -> onLockScreen()
                                "sec:settings" -> onOpenSettingsPanel()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${item.glyph} ${stringResource(item.titleRes)}",
                            color = tokens.onSurface,
                            fontSize = if (compact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subText = when (item.id) {
                            "sec:live_tv" -> if (isArabic) "32 قناة فضائية • بث حي HLS" else "32 Live Channels • HLS"
                            "sec:notifications" -> if (isArabic) "مركز التحديثات والتنبيهات" else "System Updates Hub"
                            "sec:developer" -> if (isArabic) "الملف المهني والمشاريع" else "Portfolio & Projects"
                            "sec:tv_inputs" -> if (tvState.usbDrives.isNotEmpty()) {
                                "DVB-S2 • HDMI • USB (${tvState.usbDrives.size})"
                            } else {
                                "DVB-S2 • DTV • HDMI 1-3"
                            }
                            "sec:remote_qr" -> remoteUrl.removePrefix("http://")
                            "sec:radio" -> stringResource(R.string.section_radio_live, stationsCount)
                            "sec:sports" -> {
                                val m = sportsState.featuredMatch
                                if (m != null) "${m.homeName(isArabic)} × ${m.awayName(isArabic)}" else "⚽"
                            }
                            "sec:wallpapers" -> activeWallpaperTitle
                            "sec:prayer" -> prayerState.city.displayName(isArabic)
                            "sec:adhkar" -> adhkarWindowLabel(prayerState.activeWindow)
                            "sec:weather" -> if (weatherSnapshot != null) "${weatherSnapshot.currentTempC}°C" else prayerState.city.displayName(isArabic)
                            "sec:themes" -> if (appSettings.isLightMode) "☀️ Light Glass" else "🌙 Dark Glass"
                            "sec:tools" -> "RAM • SoC • Cache"
                            "sec:lock" -> "${appSettings.lockTimeoutMin}m"
                            else -> if (isArabic) "تخصيص اللانشر" else "Customize"
                        }
                        Text(
                            text = subText,
                            color = tokens.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppsShelf(
    tokens: ThemeTokens,
    state: AppsState,
    compact: Boolean,
    glassIntensity: Float,
    isArabic: Boolean,
    firstCardFocus: FocusRequester?,
    modifier: Modifier = Modifier,
    onLaunch: (ComponentName) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleHide: (String) -> Unit,
    onMoveApp: (String, Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onRetry: () -> Unit,
) {
    var manageMode by remember { mutableStateOf(false) }
    var selectedPkg by remember { mutableStateOf<String?>(null) }

    val visibleApps = (state as? AppsState.Ready)?.visibleApps.orEmpty()
    val totalCount = (state as? AppsState.Ready)?.allApps?.size ?: 0
    val selectedApp = remember(visibleApps, selectedPkg) {
        visibleApps.firstOrNull { it.packageName == selectedPkg } ?: visibleApps.firstOrNull()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.shelf_apps),
            color = tokens.onSurface,
            fontSize = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.SemiBold,
        )

        // Inline Quick App Action Bar when manageMode is active
        if (manageMode && selectedApp != null) {
            val barShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(barShape)
                    .liquidGlass(
                        tokens = tokens,
                        baseColor = tokens.cardFocused,
                        radius = 12.dp,
                        glassIntensity = glassIntensity,
                        borderColor = tokens.primary,
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isArabic) {
                        "التحكم بتطبيق: ${selectedApp.label}"
                    } else {
                        "Manage App: ${selectedApp.label}"
                    },
                    color = tokens.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAppActionBtn(
                        id = "app_act:move_prev",
                        label = if (isArabic) "► تحريك لليمين" else "◄ Move Left",
                        tokens = tokens,
                        onClick = { onMoveApp(selectedApp.packageName, -1) },
                    )
                    QuickAppActionBtn(
                        id = "app_act:move_next",
                        label = if (isArabic) "◄ تحريك لليسار" else "Move Right ►",
                        tokens = tokens,
                        onClick = { onMoveApp(selectedApp.packageName, 1) },
                    )
                    QuickAppActionBtn(
                        id = "app_act:pin",
                        label = if (selectedApp.isPinned) {
                            if (isArabic) "★ إزالة من المفضلة" else "★ Unpin"
                        } else {
                            if (isArabic) "☆ تثبيت في المفضلة" else "☆ Pin Favorite"
                        },
                        tokens = tokens,
                        highlighted = selectedApp.isPinned,
                        onClick = { onTogglePin(selectedApp.packageName) },
                    )
                    QuickAppActionBtn(
                        id = "app_act:hide",
                        label = if (isArabic) "👁 إخفاء" else "👁 Hide",
                        tokens = tokens,
                        onClick = {
                            val pkgToHide = selectedApp.packageName
                            val nextCandidate = visibleApps.firstOrNull { it.packageName != pkgToHide }?.packageName
                            selectedPkg = nextCandidate
                            onToggleHide(pkgToHide)
                        },
                    )
                    QuickAppActionBtn(
                        id = "app_act:done",
                        label = if (isArabic) "✓ إنهاء" else "✓ Done",
                        tokens = tokens,
                        highlighted = true,
                        onClick = { manageMode = false },
                    )
                }
            }
        }

        when (state) {
            AppsState.Loading -> Unit
            is AppsState.Failed -> ErrorCard(tokens = tokens, onRetry = onRetry)
            is AppsState.Ready -> {
                val cardW = if (compact) 156.dp else 184.dp
                val cardH = if (compact) 84.dp else 102.dp
                val shape = RoundedCornerShape(tokens.cardRadius)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                ) {
                    state.visibleApps.forEachIndexed { appIdx, app ->
                        val focusMod = if (firstCardFocus != null && appIdx == 0) {
                            Modifier.focusRequester(firstCardFocus)
                        } else {
                            Modifier
                        }
                        val isSelectedForManage = manageMode && selectedApp?.packageName == app.packageName
                        AppCard(
                            tokens = tokens,
                            app = app,
                            compact = compact,
                            glassIntensity = glassIntensity,
                            isSelectedForManage = isSelectedForManage,
                            modifier = focusMod,
                            onClick = {
                                if (manageMode) {
                                    selectedPkg = app.packageName
                                } else {
                                    onLaunch(app.component)
                                }
                            },
                            onLongClick = {
                                selectedPkg = app.packageName
                                manageMode = true
                            },
                        )
                    }

                    // Action Card 1 inside the Row: Move, Pin & Hide Apps
                    val manageFocusMod = if (firstCardFocus != null && state.visibleApps.isEmpty()) {
                        Modifier.focusRequester(firstCardFocus)
                    } else {
                        Modifier
                    }
                    Column(
                        modifier = manageFocusMod
                            .width(cardW)
                            .height(cardH)
                            .mishkatFocus(id = "apps:toggle_manage")
                            .clip(shape)
                            .liquidGlass(
                                tokens = tokens,
                                baseColor = if (manageMode) tokens.cardFocused else tokens.card,
                                radius = tokens.cardRadius,
                                glassIntensity = glassIntensity,
                                borderColor = if (manageMode) tokens.primary else tokens.glassRim,
                            )
                            .clickable {
                                manageMode = !manageMode
                                if (manageMode && selectedPkg == null) {
                                    selectedPkg = visibleApps.firstOrNull()?.packageName
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "⇄",
                            color = tokens.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isArabic) {
                                if (manageMode) "✓ إنهاء الترتيب" else "تحريك ومفضلة وإخفاء"
                            } else {
                                if (manageMode) "✓ Done Editing" else "Move, Pin & Hide"
                            },
                            color = tokens.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Action Card 2 inside the Row: Open Full App Drawer & Bloatware Manager
                    Column(
                        modifier = Modifier
                            .width(cardW)
                            .height(cardH)
                            .mishkatFocus(id = "apps:open_drawer")
                            .clip(shape)
                            .liquidGlass(
                                tokens = tokens,
                                baseColor = tokens.cardFocused,
                                radius = tokens.cardRadius,
                                glassIntensity = glassIntensity,
                                borderColor = tokens.primary,
                            )
                            .clickable { onOpenDrawer() }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "⊞",
                            color = tokens.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.app_drawer_open, totalCount),
                            color = tokens.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAppActionBtn(
    id: String,
    label: String,
    tokens: ThemeTokens,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .mishkatFocus(id = id, radius = 10.dp)
            .clip(shape)
            .background(if (highlighted) tokens.cardFocused else tokens.card)
            .border(BorderStroke(1.dp, if (highlighted) tokens.primary else tokens.stroke), shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            color = if (highlighted) tokens.primary else tokens.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(
    tokens: ThemeTokens,
    app: AppEntry,
    compact: Boolean,
    glassIntensity: Float,
    isSelectedForManage: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(tokens.cardRadius)
    val iconShape = remember(tokens.iconMask) { IconMask.shapeFor(tokens.iconMask) }
    Box(
        modifier = modifier
            .width(if (compact) 156.dp else 184.dp)
            .height(if (compact) 84.dp else 102.dp)
            .mishkatFocus(id = "app:${app.key}")
            .clip(shape)
            .liquidGlass(
                tokens = tokens,
                baseColor = if (isSelectedForManage) tokens.cardFocused else tokens.card,
                radius = tokens.cardRadius,
                glassIntensity = glassIntensity,
                borderColor = if (isSelectedForManage || app.isPinned) tokens.primary else tokens.glassRim,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        if (app.banner != null) {
            Image(
                bitmap = app.banner,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    val iconSize = if (compact) 34.dp else 42.dp
                    if (app.icon != null) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(iconSize)
                                .clip(iconShape),
                        )
                    } else {
                        Spacer(Modifier.size(iconSize))
                    }
                    if (app.isPinned) {
                        Text(
                            text = "★",
                            color = tokens.primary,
                            fontSize = 12.sp,
                        )
                    }
                }
                Text(
                    text = app.label,
                    color = tokens.onSurface,
                    fontSize = if (compact) 12.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** R10: any failed card/section renders an ErrorCard instead of crashing the home screen. */
@Composable
fun ErrorCard(tokens: ThemeTokens, onRetry: () -> Unit) {
    val shape = RoundedCornerShape(tokens.cardRadius)
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(78.dp)
            .mishkatFocus(id = "error:apps")
            .clip(shape)
            .background(tokens.accentTools)
            .border(BorderStroke(1.dp, tokens.primary), shape)
            .clickable { onRetry() }
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.card_error),
            color = tokens.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** R10 / R25: Safe Mode screen shown when 3 crashes happen within 60 seconds. */
@Composable
fun SafeModeScreen(
    tokens: ThemeTokens,
    onRetry: () -> Unit,
    onOpenSystemLauncherChooser: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.surface)
            .padding(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.safe_mode_title),
                color = tokens.primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.safe_mode_body),
                color = tokens.onSurface,
                fontSize = 16.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val shape = RoundedCornerShape(tokens.cardRadius)
                Box(
                    modifier = Modifier
                        .focusRequester(firstFocus)
                        .mishkatFocus(id = "safemode:system")
                        .clip(shape)
                        .background(tokens.cardFocused)
                        .border(BorderStroke(1.dp, tokens.primary), shape)
                        .clickable { onOpenSystemLauncherChooser() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.safe_mode_system_launcher),
                        color = tokens.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "safemode:retry")
                        .clip(shape)
                        .background(tokens.card)
                        .border(BorderStroke(1.dp, tokens.stroke), shape)
                        .clickable { onRetry() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.safe_mode_retry),
                        color = tokens.onSurface,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

private fun launchApp(context: Context, component: ComponentName) {
    runCatching {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            this.component = component
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        context.startActivity(intent)
    }.onFailure { t ->
        Logger.e(Logger.TAG_HOME, t) { "Failed to launch $component" }
        Toast.makeText(context, R.string.launch_failed, Toast.LENGTH_SHORT).show()
    }
}

private fun openSystemSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { t ->
        Logger.e(Logger.TAG_HOME, t) { "Failed to open settings" }
    }
}

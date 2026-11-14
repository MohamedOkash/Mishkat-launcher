package com.mishkat.launcher.home

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mishkat.launcher.storage.StorageSourceType
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import com.mishkat.launcher.notification.NotificationRepository
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
    STORAGE_EXPLORER,
    CHANNEL_MANAGER,
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
    val storageState by graph.storageRepository.state.collectAsState()
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

    // Dynamic shelves order synced directly with user settings
    val orderedShelves = remember(appSettings.shelfOrder) {
        val valid = setOf("livetv", "apps", "prayer", "sections", "radio", "tv", "sports")
        val filtered = appSettings.shelfOrder.filter { it in valid }
        if (filtered.isNotEmpty()) filtered else listOf("apps", "prayer", "livetv")
    }
    val firstCardFocus = appsFocus

    // Single mutually-exclusive overlay state so two overlays can NEVER stack on top of each other!
    var activeOverlay by remember { mutableStateOf(ActiveOverlay.NONE) }
    var returnToNowPlaying by remember { mutableStateOf(false) }
    var lastFocusedShelfId by rememberSaveable { mutableStateOf("apps") }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 20-second automatic cycle for authentic Adhkar & Duas
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000L)
            graph.prayerRepository.nextDhikr()
        }
    }

    val anyOverlayOpen = activeOverlay != ActiveOverlay.NONE || prayerState.activeAdhanAlert != null

    // When overlay closes, smoothly restore focus back to the Home Screen after 1 frame
    var wasOverlayOpen by remember { mutableStateOf(false) }
    LaunchedEffect(anyOverlayOpen) {
        if (anyOverlayOpen) {
            wasOverlayOpen = true
        } else if (wasOverlayOpen) {
            wasOverlayOpen = false
            val targetRequester = if (returnToNowPlaying) {
                npFocus
            } else {
                shelfFocusMap[lastFocusedShelfId] ?: firstCardFocus
            }
            returnToNowPlaying = false
            repeat(4) {
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
                is CompanionCommand.OpenStorageExplorer -> activeOverlay = ActiveOverlay.STORAGE_EXPLORER
                is CompanionCommand.Key -> Unit
                else -> Unit
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

    // Keep focus strictly preserved when returning from external apps
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    repeat(3) {
                        withFrameNanos { }
                        val requester = shelfFocusMap[lastFocusedShelfId] ?: firstCardFocus
                        if (runCatching { requester.requestFocus() }.isSuccess) return@launch
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                    .focusProperties { down = appsFocus }
            ) {
                TopBar(
                    tokens = tokens,
                    hijriDate = hijriLabel,
                    weatherSnapshot = weatherSnapshot,
                    appSettings = appSettings,
                    firstFocusRequester = topBarFocus,
                    onOpenInputs = { activeOverlay = ActiveOverlay.TV_INPUTS },
                    onOpenSports = { activeOverlay = ActiveOverlay.SPORTS },
                    onOpenTools = { activeOverlay = ActiveOverlay.TOOLS },
                    onOpenAppearance = { activeOverlay = ActiveOverlay.THEMES_WALLPAPERS },
                    onOpenSettings = { activeOverlay = ActiveOverlay.SETTINGS },
                    onOpenWeather = { activeOverlay = ActiveOverlay.WEATHER },
                )
            }

            // Scrollable Shelves Container: LazyColumn ensures title + row are always scrolled and visible together
            val lazyListState = rememberLazyListState()

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                orderedShelves.forEachIndexed { index, shelfId ->
                    val isFirstShelf = index == 0
                    val shelfModifier = if (isFirstShelf) Modifier.focusProperties { up = topBarFocus } else Modifier

                    item(key = shelfId) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
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
                                        lastFocusedShelfId = "livetv"
                                        graph.liveTvRepository.playChannel(channel)
                                        activeOverlay = ActiveOverlay.LIVE_TV
                                    },
                                    onOpenCategory = { cat ->
                                        graph.liveTvRepository.selectCategory(cat)
                                    },
                                    onOpenSatellite = { sat ->
                                        graph.liveTvRepository.selectSatellite(sat)
                                    },
                                    onSyncCloud = {
                                        graph.liveTvRepository.syncFromCloudNow()
                                    },
                                    onOpenManager = {
                                        activeOverlay = ActiveOverlay.CHANNEL_MANAGER
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
                                    onLaunch = {
                                        lastFocusedShelfId = "apps"
                                        launchApp(context, it)
                                    },
                                    onTogglePin = { graph.appsRepository.togglePin(it) },
                                    onToggleHide = { graph.appsRepository.toggleHide(it) },
                                    onMoveApp = { pkg, delta -> graph.appsRepository.moveApp(pkg, delta) },
                                    onOpenDrawer = {
                                        lastFocusedShelfId = "apps"
                                        activeOverlay = ActiveOverlay.APP_MANAGER
                                    },
                                    onRetry = { graph.appsRepository.reload() },
                                )
                                "prayer" -> Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                    PrayerAndAdhkarRow(
                                        tokens = tokens,
                                        prayerState = prayerState,
                                        use24Hour = appSettings.use24Hour,
                                        firstFocusRequester = prayerFocus,
                                        onOpenCityPicker = {
                                            lastFocusedShelfId = "prayer"
                                            activeOverlay = ActiveOverlay.CITY_PICKER
                                        },
                                        onIncrementTasbih = { graph.prayerRepository.incrementTasbih() },
                                        onOpenAdhkarPanel = {
                                            lastFocusedShelfId = "prayer"
                                            activeOverlay = ActiveOverlay.ADHKAR
                                        },
                                    )
                                }
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
                                    onOpenLiveTv = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.LIVE_TV
                                    },
                                    onOpenTvInputsPanel = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.TV_INPUTS
                                    },
                                    onOpenRadioPanel = {
                                        lastFocusedShelfId = "sections"
                                        returnToNowPlaying = false
                                        activeOverlay = ActiveOverlay.RADIO
                                    },
                                    onOpenSportsPanel = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.SPORTS
                                    },
                                    onOpenRemoteQrPanel = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.REMOTE_QR
                                    },
                                    onOpenCityPicker = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.CITY_PICKER
                                    },
                                    onOpenAdhkarPanel = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.ADHKAR
                                    },
                                    onOpenWeatherPanel = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.WEATHER
                                    },
                                    onOpenThemeManager = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.THEMES_WALLPAPERS
                                    },
                                    onOpenToolsPanel = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.TOOLS
                                    },
                                    onLockScreen = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.LOCK_SCREEN
                                    },
                                    onOpenSettingsPanel = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.SETTINGS
                                    },
                                    onOpenNotifications = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.NOTIFICATIONS
                                    },
                                    onOpenDeveloperProfile = {
                                        lastFocusedShelfId = "sections"
                                        activeOverlay = ActiveOverlay.DEVELOPER_PROFILE
                                    },
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
                                            lastFocusedShelfId = "radio"
                                            returnToNowPlaying = false
                                            activeOverlay = ActiveOverlay.RADIO
                                        },
                                    )
                                }
                                "tv" -> TvInputsQuickShelf(
                                    tokens = tokens,
                                    tvState = tvInputsState,
                                    storageState = storageState,
                                    firstFocusRequester = tvInputsFocus,
                                    onSelectSource = { graph.tvInputsRepository.openSource(it) },
                                    onOpenTvHub = {
                                        lastFocusedShelfId = "tv"
                                        activeOverlay = ActiveOverlay.TV_INPUTS
                                    },
                                    onOpenStorageExplorer = {
                                        lastFocusedShelfId = "tv"
                                        activeOverlay = ActiveOverlay.STORAGE_EXPLORER
                                    },
                                )
                                "sports" -> SportsQuickShelf(
                                    tokens = tokens,
                                    sportsState = sportsState,
                                    firstFocusRequester = sportsFocus,
                                    onOpenSportsCenter = {
                                        lastFocusedShelfId = "sports"
                                        activeOverlay = ActiveOverlay.SPORTS
                                    },
                                    onCycleFavoriteTeam = { graph.sportsRepository.cycleFavoriteTeam() },
                                )
                            }
                        }
                    }
                }

                // Compact Hero Billboard Carousel (S3 + R23)
                item(key = "billboard") {
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
                                    lastFocusedShelfId = "radio"
                                    returnToNowPlaying = false
                                    activeOverlay = ActiveOverlay.RADIO
                                }
                                1 -> {
                                    lastFocusedShelfId = "prayer"
                                    activeOverlay = ActiveOverlay.ADHKAR
                                }
                                2 -> {
                                    lastFocusedShelfId = "prayer"
                                    activeOverlay = ActiveOverlay.CITY_PICKER
                                }
                                else -> {
                                    lastFocusedShelfId = "sports"
                                    activeOverlay = ActiveOverlay.SPORTS
                                }
                            }
                        },
                    )
                }
            }
        }

        // Mutually-exclusive isolated dialog overlays (prevents any overlay stacking & isolates focus)
        if (activeOverlay != ActiveOverlay.NONE) {
            Dialog(
                onDismissRequest = { activeOverlay = ActiveOverlay.NONE },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    decorFitsSystemWindows = false,
                ),
            ) {
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
                        selectedMuezzin = prayerState.selectedMuezzin,
                        isAdhanTesting = prayerState.isAdhanTesting,
                        onSelectCity = { graph.prayerRepository.selectCity(it) },
                        onSelectMuezzin = { graph.prayerRepository.setMuezzin(it) },
                        onTestAdhan = { graph.prayerRepository.testAdhanToggle() },
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
                        autoWallpaperManager = graph.autoWallpaperManager,
                        onSelectTheme = {
                            graph.themeRepository.select(it)
                            graph.localeRepository.setWallpaperStyle("auto")
                        },
                        onSelectWallpaper = { graph.localeRepository.setWallpaperStyle(it) },
                        onCycleGlassIntensity = { graph.localeRepository.cycleGlassIntensity() },
                        onToggleLightMode = { graph.localeRepository.toggleLightMode() },
                        onDismiss = { activeOverlay = ActiveOverlay.NONE },
                    )
                    ActiveOverlay.TOOLS -> DeviceToolsOverlay(
                        tokens = tokens,
                        killer = graph.backgroundProcessKiller,
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
                        prayerState = prayerState,
                        cities = cities,
                        backgroundKiller = graph.backgroundProcessKiller,
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
                        onSelectCity = { graph.prayerRepository.selectCity(it) },
                        onSelectMuezzin = { graph.prayerRepository.setMuezzin(it) },
                        onTestAdhan = { graph.prayerRepository.testAdhanToggle() },
                        onTriggerAdhanPreview = { graph.prayerRepository.triggerAdhanPreview() },
                        onToggleAudioAdhan = { graph.prayerRepository.setAudioAdhanEnabled(it) },
                        onOpenDiagnostics = { activeOverlay = ActiveOverlay.TOOLS },
                        onOpenInputsHub = { activeOverlay = ActiveOverlay.TV_INPUTS },
                        onOpenStorageExplorer = { activeOverlay = ActiveOverlay.STORAGE_EXPLORER },
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
                    ActiveOverlay.STORAGE_EXPLORER -> com.mishkat.launcher.storage.StorageExplorerOverlay(
                        repository = graph.storageRepository,
                        onDismiss = { activeOverlay = ActiveOverlay.NONE },
                    )
                    ActiveOverlay.CHANNEL_MANAGER -> com.mishkat.launcher.livetv.ChannelAndTransponderManagerModal(
                        tokens = tokens,
                        repository = graph.liveTvRepository,
                        isArabic = isArabic,
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
            }
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
    hijriDate: String,
    weatherSnapshot: WeatherSnapshot?,
    appSettings: AppSettings,
    firstFocusRequester: FocusRequester,
    onOpenInputs: () -> Unit,
    onOpenSports: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenSettings: () -> Unit,
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

    val isArabic = appSettings.isArabic
    val pillShape = RoundedCornerShape(12.dp)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Action Navigation Pills (Apple TV tvOS Glass Style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Pill 1: Inputs & Storage
                TopBarActionPill(
                    id = "topbar:btn_inputs",
                    label = if (isArabic) "📂 المداخل" else "📂 Inputs",
                    tokens = tokens,
                    glassIntensity = glass,
                    shape = pillShape,
                    focusRequester = firstFocusRequester,
                    onClick = onOpenInputs,
                )

                // Pill 2: Sports Center
                TopBarActionPill(
                    id = "topbar:btn_sports",
                    label = if (isArabic) "⚽ المباريات" else "⚽ Sports",
                    tokens = tokens,
                    glassIntensity = glass,
                    shape = pillShape,
                    onClick = onOpenSports,
                )

                // Pill 3: System Tools & Maintenance
                TopBarActionPill(
                    id = "topbar:btn_tools",
                    label = if (isArabic) "🛠️ الصيانة" else "🛠️ Maintenance",
                    tokens = tokens,
                    glassIntensity = glass,
                    shape = pillShape,
                    onClick = onOpenTools,
                )

                // Pill 4: Themes & Appearance
                TopBarActionPill(
                    id = "topbar:btn_appearance",
                    label = if (isArabic) "🎨 المظهر" else "🎨 Appearance",
                    tokens = tokens,
                    glassIntensity = glass,
                    shape = pillShape,
                    onClick = onOpenAppearance,
                )
            }

            // Right Group: Clock & Weather Capsule + Logo Capsule + Settings Capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Clock & Weather Capsule
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "topbar:clock_weather", radius = 12.dp)
                        .clip(pillShape)
                        .background(Color(0xEE090E1A))
                        .border(1.dp, tokens.glassRim.copy(alpha = 0.40f), pillShape)
                        .clickable { onOpenWeather() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            text = timeText,
                            color = tokens.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 10f)),
                        )
                        if (weatherSnapshot != null) {
                            Text(
                                text = "| ${weatherCodeGlyph(weatherSnapshot.currentWeatherCode)} ${weatherSnapshot.currentTempC}°C",
                                color = tokens.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        if (hijriDate.isNotBlank()) {
                            Text(
                                text = "| $hijriDate",
                                color = tokens.onSurfaceMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                // Brand Logo Capsule with Mishkat Lamp Emblem
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(Color(0xEE090E1A))
                        .border(1.5.dp, tokens.primary, pillShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "✦",
                            color = tokens.primary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = "مشكاة",
                            color = tokens.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(tokens.primary),
                        )
                    }
                }

                // Dedicated Settings Pill Button (⚙ الضبط) placed right next to Mishkat
                TopBarActionPill(
                    id = "topbar:btn_settings",
                    label = if (isArabic) "⚙ الضبط" else "⚙ Settings",
                    tokens = tokens,
                    glassIntensity = glass,
                    shape = pillShape,
                    highlighted = true,
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun TopBarActionPill(
    id: String,
    label: String,
    tokens: ThemeTokens,
    glassIntensity: Float,
    shape: RoundedCornerShape,
    highlighted: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val focusMod = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    Box(
        modifier = focusMod
            .mishkatFocus(id = id, radius = 12.dp)
            .focusable()
            .clip(shape)
            .liquidGlass(
                tokens = tokens,
                baseColor = if (highlighted) tokens.cardFocused else tokens.card,
                radius = 12.dp,
                glassIntensity = glassIntensity,
                borderColor = if (highlighted) tokens.primary else tokens.glassRim.copy(alpha = 0.45f),
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (highlighted) tokens.primary else tokens.onSurface,
            fontSize = 13.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.mishkat.launcher.theme.SectionTitlePill(
            title = stringResource(R.string.shelf_sections),
            tokens = tokens,
            icon = "✦",
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(modifier = Modifier.width(24.dp))
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
    val context = LocalContext.current

    val visibleApps = (state as? AppsState.Ready)?.visibleApps.orEmpty()
    val totalCount = (state as? AppsState.Ready)?.allApps?.size ?: 0
    val selectedApp = remember(visibleApps, selectedPkg) {
        visibleApps.firstOrNull { it.packageName == selectedPkg } ?: visibleApps.firstOrNull()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        com.mishkat.launcher.theme.SectionTitlePill(
            title = stringResource(R.string.shelf_apps),
            tokens = tokens,
            icon = "📱",
            badgeText = if (totalCount > 0) "$totalCount" else null,
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
                        id = "app_act:settings",
                        label = if (isArabic) "🧹 مسح الكاش وإيقاف" else "🧹 App Info",
                        tokens = tokens,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${selectedApp.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        },
                    )
                    QuickAppActionBtn(
                        id = "app_act:uninstall",
                        label = if (isArabic) "🗑️ إلغاء التثبيت" else "🗑️ Uninstall",
                        tokens = tokens,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_DELETE).apply {
                                        data = android.net.Uri.parse("package:${selectedApp.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
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

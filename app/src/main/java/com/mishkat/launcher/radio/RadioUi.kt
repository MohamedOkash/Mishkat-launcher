package com.mishkat.launcher.radio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.mishkat.launcher.i18n.LocalAppSettings
import com.mishkat.launcher.system.SleepTimerPolicy
import com.mishkat.launcher.system.SleepTimerState
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass

@Composable
fun NowPlayingBar(
    tokens: ThemeTokens,
    radioState: RadioUiState,
    sleepState: SleepTimerState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onOpenRadioPanel: () -> Unit,
    panelTriggerFocus: FocusRequester,
) {
    val appSettings = LocalAppSettings.current
    val isArabic = appSettings.isArabic
    val compact = appSettings.compactLayout
    val glass = appSettings.glassIntensity
    val station = radioState.currentStation
    val title = station?.displayName(isArabic) ?: stringResource(R.string.section_radio)
    val statusText = when {
        radioState.errorFallbackActive -> stringResource(R.string.radio_error_fallback)
        radioState.isBuffering -> stringResource(R.string.radio_buffering)
        radioState.isPlaying -> stringResource(R.string.radio_now_playing)
        else -> stringResource(R.string.radio_idle)
    }

    val shape = RoundedCornerShape(tokens.cardRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .liquidGlass(tokens, tokens.card, tokens.cardRadius, glass)
            .padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Station Badge + Name (clicking opens the full 178-station panel)
        val openPanelCd = stringResource(R.string.cd_open_radio_panel)
        Row(
            modifier = Modifier
                .weight(1f)
                .focusRequester(panelTriggerFocus)
                .mishkatFocus(id = "np:station", radius = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenRadioPanel() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics { contentDescription = openPanelCd },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReciterMonogram(tokens = tokens, name = title, isPlaying = radioState.isPlaying)

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    color = tokens.onSurface,
                    fontSize = if (compact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusText,
                    color = if (radioState.isPlaying) tokens.primary else tokens.onSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Transport + Favorite + Sleep Timer controls (R20: contentDescription on every action button)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionPill(
                id = "np:prev",
                label = "⏮",
                cd = stringResource(R.string.cd_radio_prev),
                tokens = tokens,
                highlighted = false,
                onClick = onPrev,
            )

            ActionPill(
                id = "np:play",
                label = if (radioState.isPlaying || radioState.isBuffering) {
                    stringResource(R.string.radio_pause)
                } else {
                    stringResource(R.string.radio_play)
                },
                cd = stringResource(R.string.cd_radio_play_pause),
                tokens = tokens,
                highlighted = true,
                onClick = onTogglePlayPause,
            )

            ActionPill(
                id = "np:next",
                label = "⏭",
                cd = stringResource(R.string.cd_radio_next),
                tokens = tokens,
                highlighted = false,
                onClick = onNext,
            )

            val isFav = station?.isFavorite == true
            ActionPill(
                id = "np:fav",
                label = if (isFav) "★" else "☆",
                cd = stringResource(R.string.cd_radio_favorite),
                tokens = tokens,
                highlighted = isFav,
                onClick = onToggleFavorite,
            )

            val sleepLabel = when {
                sleepState.isFading -> stringResource(R.string.sleep_timer_fading)
                sleepState.active -> stringResource(
                    R.string.sleep_timer_active,
                    SleepTimerPolicy.formatRemaining(sleepState.remainingMs),
                )
                else -> stringResource(R.string.sleep_timer_off)
            }
            ActionPill(
                id = "np:sleep",
                label = sleepLabel,
                cd = stringResource(R.string.cd_sleep_timer),
                tokens = tokens,
                highlighted = sleepState.active,
                onClick = onCycleSleepTimer,
            )
        }
    }
}

@Composable
fun RecitersQuickShelf(
    tokens: ThemeTokens,
    stations: List<StationEntity>,
    currentStationId: Int?,
    isPlaying: Boolean,
    firstFocusRequester: FocusRequester? = null,
    onSelectStation: (StationEntity) -> Unit,
    onOpenAll: () -> Unit,
) {
    val appSettings = LocalAppSettings.current
    val isArabic = appSettings.isArabic
    val compact = appSettings.compactLayout
    val glass = appSettings.glassIntensity
    val preview = remember(stations) { stations.take(16) }
    val firstStationId = preview.firstOrNull()?.id

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.mishkat.launcher.theme.SectionTitlePill(
            title = stringResource(R.string.shelf_reciters),
            tokens = tokens,
            icon = "📻",
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = preview,
                key = { it.id },
                contentType = { "reciter" },
            ) { station ->
                val active = station.id == currentStationId
                val isFirst = station.id == firstStationId
                val shape = RoundedCornerShape(tokens.cardRadius)
                val name = station.displayName(isArabic)
                Row(
                    modifier = (if (isFirst && firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
                        .width(if (compact) 196.dp else 224.dp)
                        .height(if (compact) 64.dp else 74.dp)
                        .mishkatFocus(id = "reciter:${station.id}")
                        .clip(shape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = if (active) tokens.cardFocused else tokens.card,
                            radius = tokens.cardRadius,
                            glassIntensity = glass,
                            borderColor = if (active) tokens.primary else tokens.glassRim,
                        )
                        .clickable { onSelectStation(station) }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    ReciterMonogram(
                        tokens = tokens,
                        name = name,
                        isPlaying = active && isPlaying,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = name,
                            color = tokens.onSurface,
                            fontSize = if (compact) 12.sp else 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (active && isPlaying) {
                                stringResource(R.string.radio_now_playing)
                            } else if (station.isFavorite) {
                                "★ مفضلة"
                            } else {
                                "📻 بث مباشر"
                            },
                            color = if (active || station.isFavorite) tokens.primary else tokens.onSurfaceMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            item(key = "radio_open_all_card") {
                val shape = RoundedCornerShape(tokens.cardRadius)
                Column(
                    modifier = Modifier
                        .width(if (compact) 176.dp else 200.dp)
                        .height(if (compact) 64.dp else 74.dp)
                        .mishkatFocus(id = "radio:see_all")
                        .clip(shape)
                        .liquidGlass(tokens, tokens.cardFocused, tokens.cardRadius, glass, tokens.primary)
                        .clickable { onOpenAll() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.radio_all_stations, stations.size),
                        color = tokens.primary,
                        fontSize = if (compact) 13.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = if (isArabic) "تصفح وبحث كل الإذاعات" else "Browse all stations",
                        color = tokens.onSurface,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Full-screen overlay panel listing all 177 stations + Favorites filter + online refresh.
 * R15: restores focus to the originating element when closed.
 */
@Composable
fun RadioPanelOverlay(
    tokens: ThemeTokens,
    stations: List<StationEntity>,
    radioState: RadioUiState,
    sleepState: SleepTimerState,
    onPlayStation: (StationEntity) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onCycleSleepTimer: () -> Unit,
    onRefreshOnline: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val isArabic = LocalAppSettings.current.isArabic
    var activeFilter by remember { mutableStateOf(0) } // 0=All, 1=Favorites, 2=Reciters, 3=Thematic
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    val firstStationFocus = remember { FocusRequester() }

    val favoritesCount = remember(stations) { stations.count { it.isFavorite } }
    val recitersCount = remember(stations) { stations.count { !it.isThematicChannel } }
    val thematicCount = remember(stations) { stations.count { it.isThematicChannel } }

    val availableLetters = remember(stations, isArabic) {
        stations.mapNotNull { s ->
            s.displayName(isArabic).trim().firstOrNull { it.isLetter() }
        }.distinct().take(18)
    }

    val filtered = remember(stations, activeFilter, selectedLetter, isArabic) {
        val byTab = when (activeFilter) {
            1 -> stations.filter { it.isFavorite }
            2 -> stations.filter { !it.isThematicChannel }
            3 -> stations.filter { it.isThematicChannel }
            else -> stations
        }
        val letter = selectedLetter
        if (letter == null) {
            byTab
        } else {
            byTab.filter { it.displayName(isArabic).trim().startsWith(letter, ignoreCase = true) }
        }
    }

    LaunchedEffect(Unit) {
        repeat(4) {
            withFrameNanos { }
            if (runCatching { firstStationFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.surface.copy(alpha = 0.96f))
            .padding(horizontal = 56.dp, vertical = 36.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header + tabs + sleep timer + refresh + close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.shelf_reciters),
                        color = tokens.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    ActionPill(
                        id = "panel:tab_all",
                        label = stringResource(R.string.radio_all_stations, stations.size),
                        cd = stringResource(R.string.radio_all_stations, stations.size),
                        tokens = tokens,
                        highlighted = activeFilter == 0,
                        onClick = {
                            activeFilter = 0
                            selectedLetter = null
                        },
                    )

                    ActionPill(
                        id = "panel:tab_fav",
                        label = stringResource(R.string.radio_favorites_tab, favoritesCount),
                        cd = stringResource(R.string.radio_favorites_tab, favoritesCount),
                        tokens = tokens,
                        highlighted = activeFilter == 1,
                        onClick = {
                            activeFilter = 1
                            selectedLetter = null
                        },
                    )

                    ActionPill(
                        id = "panel:tab_reciters",
                        label = stringResource(R.string.radio_reciters_tab, recitersCount),
                        cd = stringResource(R.string.radio_reciters_tab, recitersCount),
                        tokens = tokens,
                        highlighted = activeFilter == 2,
                        onClick = {
                            activeFilter = 2
                            selectedLetter = null
                        },
                    )

                    ActionPill(
                        id = "panel:tab_thematic",
                        label = stringResource(R.string.radio_thematic_tab, thematicCount),
                        cd = stringResource(R.string.radio_thematic_tab, thematicCount),
                        tokens = tokens,
                        highlighted = activeFilter == 3,
                        onClick = {
                            activeFilter = 3
                            selectedLetter = null
                        },
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val sleepLabel = if (sleepState.active) {
                        stringResource(
                            R.string.sleep_timer_active,
                            SleepTimerPolicy.formatRemaining(sleepState.remainingMs),
                        )
                    } else {
                        stringResource(R.string.sleep_timer_off)
                    }
                    ActionPill(
                        id = "panel:sleep",
                        label = sleepLabel,
                        cd = stringResource(R.string.cd_sleep_timer),
                        tokens = tokens,
                        highlighted = sleepState.active,
                        onClick = onCycleSleepTimer,
                    )

                    ActionPill(
                        id = "panel:refresh",
                        label = stringResource(R.string.radio_refresh),
                        cd = stringResource(R.string.radio_refresh),
                        tokens = tokens,
                        highlighted = false,
                        onClick = onRefreshOnline,
                    )

                    ActionPill(
                        id = "panel:close",
                        label = stringResource(R.string.radio_close),
                        cd = stringResource(R.string.radio_close),
                        tokens = tokens,
                        highlighted = false,
                        onClick = onDismiss,
                    )
                }
            }

            // Quick alphabet index row for instant reciter jump on 10-foot TV UI
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(
                    items = availableLetters,
                    key = { it.code },
                    contentType = { "letter_chip" },
                ) { ch ->
                    val active = selectedLetter == ch
                    ActionPill(
                        id = "panel:letter_$ch",
                        label = ch.toString(),
                        cd = ch.toString(),
                        tokens = tokens,
                        highlighted = active,
                        onClick = { selectedLetter = if (active) null else ch },
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.radio_favorites_empty),
                        color = tokens.onSurfaceMuted,
                        fontSize = 18.sp,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = filtered,
                        key = { it.id },
                        contentType = { "panel_station" },
                    ) { station ->
                        val active = station.id == radioState.currentStation?.id
                        val shape = RoundedCornerShape(tokens.cardRadius)
                        val name = station.displayName(isArabic)
                        val isFirst = station.id == filtered.first().id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = (if (isFirst) Modifier.focusRequester(firstStationFocus) else Modifier)
                                    .weight(1f)
                                    .mishkatFocus(id = "panel_station:${station.id}")
                                    .clip(shape)
                                    .background(if (active) tokens.cardFocused else tokens.card)
                                    .border(
                                        BorderStroke(1.dp, if (active) tokens.primary else tokens.stroke),
                                        shape,
                                    )
                                    .clickable { onPlayStation(station) }
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    ReciterMonogram(
                                        tokens = tokens,
                                        name = name,
                                        isPlaying = active && radioState.isPlaying,
                                    )
                                    Text(
                                        text = name,
                                        color = tokens.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                if (active) {
                                    Text(
                                        text = stringResource(
                                            if (radioState.isBuffering) R.string.radio_buffering
                                            else if (radioState.isPlaying) R.string.radio_now_playing
                                            else R.string.radio_paused,
                                        ),
                                        color = tokens.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }

                            ActionPill(
                                id = "panel_fav:${station.id}",
                                label = if (station.isFavorite) "★" else "☆",
                                cd = stringResource(
                                    if (station.isFavorite) R.string.radio_favorite_remove
                                    else R.string.radio_favorite_add,
                                ),
                                tokens = tokens,
                                highlighted = station.isFavorite,
                                onClick = { onToggleFavorite(station.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReciterMonogram(tokens: ThemeTokens, name: String, isPlaying: Boolean) {
    val badgeLetter = remember(name) {
        name.trim().firstOrNull { it.isLetter() }?.toString() ?: "ق"
    }
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isPlaying) tokens.primary else tokens.cardFocused)
            .border(BorderStroke(1.dp, tokens.primary), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = badgeLetter,
            color = if (isPlaying) tokens.surface else tokens.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ActionPill(
    id: String,
    label: String,
    cd: String,
    tokens: ThemeTokens,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .mishkatFocus(id = id, radius = 14.dp)
            .clip(shape)
            .background(if (highlighted) tokens.cardFocused else tokens.card)
            .border(
                BorderStroke(1.dp, if (highlighted) tokens.primary else tokens.stroke),
                shape,
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (highlighted) tokens.primary else tokens.onSurface,
            fontSize = 14.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

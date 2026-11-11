package com.mishkat.launcher.sports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.LocalAppSettings
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass

/**
 * S7: Compact Liquid Glass Sports & Match Center shelf on the Home Screen (shown in Sports Arena theme).
 */
@Composable
fun SportsQuickShelf(
    tokens: ThemeTokens,
    sportsState: SportsUiState,
    firstFocusRequester: FocusRequester? = null,
    onOpenSportsCenter: () -> Unit,
    onCycleFavoriteTeam: () -> Unit,
) {
    val appSettings = LocalAppSettings.current
    val isArabic = appSettings.isArabic
    val compact = appSettings.compactLayout
    val glass = appSettings.glassIntensity

    val favOption = remember(sportsState.favoriteTeamKey) {
        FAVORITE_TEAMS_PRESETS.firstOrNull { it.keyEn == sportsState.favoriteTeamKey }
            ?: FAVORITE_TEAMS_PRESETS.first()
    }
    val preview = remember(sportsState.matches, sportsState.favoriteTeamKey) {
        val fav = sportsState.favoriteTeamKey
        val prioritized = if (fav.isNotBlank()) {
            sportsState.matches.sortedByDescending { it.involvesTeam(fav) }
        } else {
            sportsState.matches
        }
        prioritized.take(12)
    }
    val firstMatchId = preview.firstOrNull()?.id
    val hasLive = remember(preview) { preview.any { it.isLive } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.mishkat.launcher.theme.SectionTitlePill(
            title = stringResource(R.string.sports_shelf_title),
            tokens = tokens,
            icon = "⚽",
            badgeText = if (hasLive) "LIVE" else null,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.width(24.dp))
            preview.forEach { match ->
                val shape = RoundedCornerShape(tokens.cardRadius)
                val isFirst = match.id == firstMatchId
                val isFavMatch = sportsState.favoriteTeamKey.isNotBlank() &&
                    match.involvesTeam(sportsState.favoriteTeamKey)
                val winner = match.winnerName(isArabic)

                Column(
                    modifier = (if (isFirst && firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
                        .width(if (compact) 235.dp else 265.dp)
                        .height(if (compact) 82.dp else 94.dp)
                        .mishkatFocus(id = "match_quick:${match.id}")
                        .clip(shape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = if (isFavMatch || match.isLive) tokens.cardFocused else tokens.card,
                            radius = tokens.cardRadius,
                            glassIntensity = glass,
                            borderColor = if (isFavMatch || match.isLive) tokens.primary else tokens.glassRim,
                        )
                        .clickable { onOpenSportsCenter() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Top: League Name + Status / Winner Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = match.leagueName(isArabic),
                            color = tokens.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = when {
                                match.isLive -> "● LIVE"
                                winner != null -> "🏆 $winner"
                                else -> match.statusText
                            },
                            color = if (match.isLive) Color(0xFFEF4444) else if (winner != null) tokens.primary else tokens.onSurfaceMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }

                    // Middle: Home Team Row (Name + Score)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = match.homeName(isArabic),
                            color = tokens.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (match.isLive || match.isFinished) {
                            Text(
                                text = match.homeScore.toString(),
                                color = if (match.isLive) tokens.primary else tokens.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Bottom: Away Team Row (Name + Score)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = match.awayName(isArabic),
                            color = tokens.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (match.isLive || match.isFinished) {
                            Text(
                                text = match.awayScore.toString(),
                                color = if (match.isLive) tokens.primary else tokens.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            val favShape = RoundedCornerShape(tokens.cardRadius)
            Column(
                modifier = Modifier
                    .width(if (compact) 180.dp else 204.dp)
                    .height(if (compact) 82.dp else 94.dp)
                    .mishkatFocus(id = "sports:fav_team_quick")
                    .clip(favShape)
                    .liquidGlass(tokens, tokens.cardFocused, tokens.cardRadius, glass, tokens.primary)
                    .clickable { onCycleFavoriteTeam() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "★ ${favOption.displayName(isArabic)}",
                    color = tokens.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = if (isArabic) "اضغط لتغيير فريقك المفضل" else "Click to cycle favorite team",
                    color = tokens.onSurface,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            Column(
                modifier = Modifier
                    .width(if (compact) 180.dp else 204.dp)
                    .height(if (compact) 68.dp else 80.dp)
                    .mishkatFocus(id = "sports:see_all")
                    .clip(favShape)
                    .liquidGlass(tokens, tokens.card, tokens.cardRadius, glass)
                    .clickable { onOpenSportsCenter() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.sports_open_center, sportsState.matches.size),
                    color = tokens.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = if (isArabic) "جدول المباريات والفائزين" else "All fixtures & winners",
                    color = tokens.onSurface,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * S7: Full Match Center & Teams Overlay.
 */
@Composable
fun SportsPanelOverlay(
    tokens: ThemeTokens,
    sportsState: SportsUiState,
    onSelectFavoriteTeam: (String) -> Unit,
    onRefresh: () -> Unit,
    onApplySportsTheme: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val appSettings = LocalAppSettings.current
    val isArabic = appSettings.isArabic
    val glass = appSettings.glassIntensity
    var tab by remember { mutableIntStateOf(0) } // 0=All, 1=FavTeam, 2=Upcoming, 3=Finished/Winners
    val firstFocus = remember { FocusRequester() }

    val filtered = remember(sportsState.matches, sportsState.favoriteTeamKey, tab) {
        when (tab) {
            1 -> sportsState.matches.filter { it.involvesTeam(sportsState.favoriteTeamKey) }
            2 -> sportsState.matches.filter { it.isUpcoming || it.isLive }
            3 -> sportsState.matches.filter { it.isFinished }
            else -> sportsState.matches
        }
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
            .background(tokens.surface.copy(alpha = 0.94f))
            .padding(horizontal = 44.dp, vertical = 26.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.sports_panel_title),
                        color = tokens.primary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    SportsTabPill(
                        id = "sports_tab:all",
                        label = stringResource(R.string.sports_tab_all, sportsState.matches.size),
                        tokens = tokens,
                        glass = glass,
                        highlighted = tab == 0,
                        modifier = Modifier.focusRequester(firstFocus),
                        onClick = { tab = 0 },
                    )
                    SportsTabPill(
                        id = "sports_tab:fav",
                        label = stringResource(R.string.sports_tab_fav),
                        tokens = tokens,
                        glass = glass,
                        highlighted = tab == 1,
                        onClick = { tab = 1 },
                    )
                    SportsTabPill(
                        id = "sports_tab:upcoming",
                        label = stringResource(R.string.sports_tab_upcoming),
                        tokens = tokens,
                        glass = glass,
                        highlighted = tab == 2,
                        onClick = { tab = 2 },
                    )
                    SportsTabPill(
                        id = "sports_tab:winners",
                        label = stringResource(R.string.sports_tab_winners),
                        tokens = tokens,
                        glass = glass,
                        highlighted = tab == 3,
                        onClick = { tab = 3 },
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tokens.id != "sports_arena") {
                        SportsTabPill(
                            id = "sports:apply_theme",
                            label = stringResource(R.string.sports_activate_theme),
                            tokens = tokens,
                            glass = glass,
                            highlighted = true,
                            onClick = onApplySportsTheme,
                        )
                    }

                    SportsTabPill(
                        id = "sports:refresh",
                        label = stringResource(R.string.radio_refresh),
                        tokens = tokens,
                        glass = glass,
                        highlighted = false,
                        onClick = onRefresh,
                    )

                    SportsTabPill(
                        id = "sports:close",
                        label = stringResource(R.string.radio_close),
                        tokens = tokens,
                        glass = glass,
                        highlighted = false,
                        onClick = onDismiss,
                    )
                }
            }

            // Favorite Team Selector Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(
                    items = FAVORITE_TEAMS_PRESETS,
                    key = { it.keyEn.ifBlank { "all" } },
                ) { option ->
                    val selected = option.keyEn == sportsState.favoriteTeamKey
                    SportsTabPill(
                        id = "sports_team:${option.keyEn.ifBlank { "all" }}",
                        label = if (selected) "★ ${option.displayName(isArabic)}" else option.displayName(isArabic),
                        tokens = tokens,
                        glass = glass,
                        highlighted = selected,
                        onClick = { onSelectFavoriteTeam(option.keyEn) },
                    )
                }
            }

            // Matches List
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = if (isArabic) "لا توجد مباريات جارية حالياً لهذا اليوم" else "No matches scheduled for today",
                            color = tokens.onSurfaceMuted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val btnShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .mishkatFocus(id = "sports:empty_refresh", radius = 12.dp)
                                .clip(btnShape)
                                .liquidGlass(tokens, tokens.cardFocused, 12.dp, glass, tokens.primary)
                                .clickable { onRefresh() }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = if (isArabic) "🔄 تحديث الجدول الآن" else "🔄 Refresh Schedule",
                                color = tokens.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 18.dp),
                ) {
                    items(
                        items = filtered,
                        key = { it.id },
                        contentType = { "match_row" },
                    ) { match ->
                        val shape = RoundedCornerShape(tokens.cardRadius)
                        val winner = match.winnerName(isArabic)
                        val statusBadge = when {
                            match.isLive -> "● LIVE (${match.statusText})"
                            winner != null -> stringResource(R.string.sports_winner_fmt, winner)
                            match.isFinished -> stringResource(R.string.sports_draw_label)
                            else -> match.statusText
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .mishkatFocus(id = "match_row:${match.id}")
                                .clip(shape)
                                .liquidGlass(
                                    tokens = tokens,
                                    baseColor = if (match.isLive) tokens.cardFocused else tokens.card,
                                    radius = tokens.cardRadius,
                                    glassIntensity = glass,
                                    borderColor = if (match.isLive || winner != null) tokens.primary else tokens.glassRim,
                                )
                                .clickable { onSelectFavoriteTeam(match.homeEn) }
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = match.leagueName(isArabic),
                                    color = tokens.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = match.homeName(isArabic),
                                        color = tokens.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = if (match.isLive || match.isFinished) "${match.homeScore} - ${match.awayScore}" else "VS",
                                        color = tokens.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = match.awayName(isArabic),
                                        color = tokens.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Text(
                                text = statusBadge,
                                color = if (match.isLive) Color(0xFFEF4444) else tokens.onSurfaceMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SportsTabPill(
    id: String,
    label: String,
    tokens: ThemeTokens,
    glass: Float,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .mishkatFocus(id = id, radius = 12.dp)
            .clip(shape)
            .liquidGlass(
                tokens = tokens,
                baseColor = if (highlighted) tokens.cardFocused else tokens.card,
                radius = 12.dp,
                glassIntensity = glass,
                borderColor = if (highlighted) tokens.primary else tokens.glassRim,
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (highlighted) tokens.primary else tokens.onSurface,
            fontSize = 12.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

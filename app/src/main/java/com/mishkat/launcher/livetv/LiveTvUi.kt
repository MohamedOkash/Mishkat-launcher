package com.mishkat.launcher.livetv

import android.media.tv.TvView
import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass
import kotlinx.coroutines.delay

@Composable
fun LiveTvQuickShelf(
    tokens: ThemeTokens,
    state: LiveTvUiState,
    compact: Boolean,
    glassIntensity: Float,
    isArabic: Boolean,
    firstFocusRequester: FocusRequester? = null,
    onSelectChannel: (LiveChannel) -> Unit,
    onOpenCategory: (ChannelCategory) -> Unit,
    onOpenSatellite: (SatelliteSource) -> Unit = {},
    onSyncCloud: () -> Unit = {},
    onOpenManager: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val shape = RoundedCornerShape(tokens.cardRadius)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Row 1: Shelf Header: Title + LIVE badge + Cloud Sync & Channel Count Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.mishkat.launcher.theme.SectionTitlePill(
                title = if (isArabic) "دليل القنوات الفضائية والساتلايت" else "Live Satellite Channels",
                tokens = tokens,
                icon = "📺",
                badgeText = "LIVE",
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Channel & Transponder Manager Button
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "livetv:open_mgr", radius = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .liquidGlass(tokens, tokens.card, 8.dp, glassIntensity)
                        .clickable { onOpenManager() }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = if (isArabic) "⚙️ إدارة الترددات" else "⚙️ Manage",
                        color = tokens.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Cloud Freq Sync Button
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "livetv:sync_freqs", radius = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .liquidGlass(tokens, tokens.card, 8.dp, glassIntensity)
                        .clickable { onSyncCloud() }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = if (state.isSyncingCloud) (if (isArabic) "⏳ تحديث..." else "⏳ Syncing...")
                               else (if (isArabic) "☁️ تحديث الترددات" else "☁️ Sync Freqs"),
                        color = tokens.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Quick channel count pill
                val count = state.filteredChannels.size
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .liquidGlass(tokens, tokens.card, 8.dp, glassIntensity)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = if (isArabic) "$count قناة متاحة" else "$count Channels",
                        color = tokens.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Row 2: Unified Filter Bar (Satellites + Categories in one sleek horizontal row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SatelliteSource.entries.forEach { sat ->
                val isSelected = sat == state.activeSatellite
                val pillShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "livetv:sat:${sat.id}", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = if (isSelected) tokens.primary.copy(alpha = 0.35f) else tokens.card,
                            radius = 10.dp,
                            glassIntensity = glassIntensity,
                            borderColor = if (isSelected) tokens.primary else tokens.glassRim,
                        )
                        .clickable { onOpenSatellite(sat) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${sat.glyph} ${if (isArabic) sat.titleAr else sat.titleEn}",
                        color = if (isSelected) tokens.primary else tokens.onSurface,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(tokens.glassRim.copy(alpha = 0.5f)),
            )

            ChannelCategory.entries.forEach { cat ->
                val isSelected = cat == state.activeCategory
                val pillShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "livetv:cat:${cat.id}", radius = 10.dp)
                        .clip(pillShape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = if (isSelected) tokens.cardFocused else tokens.card,
                            radius = 10.dp,
                            glassIntensity = glassIntensity,
                            borderColor = if (isSelected) tokens.primary else tokens.glassRim,
                        )
                        .clickable { onOpenCategory(cat) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${cat.glyph} ${if (isArabic) cat.titleAr else cat.titleEn}",
                        color = if (isSelected) tokens.primary else tokens.onSurface,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        // Horizontal Row of Channel Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(modifier = Modifier.width(4.dp))
            val displayedChannels = state.filteredChannels.ifEmpty { state.channels }
            displayedChannels.forEachIndexed { index, channel ->
                val isFirst = index == 0
                val cardFocusMod = if (isFirst && firstFocusRequester != null) {
                    Modifier.focusRequester(firstFocusRequester)
                } else {
                    Modifier
                }
                val cardWidth = if (isFirst) {
                    if (compact) 220.dp else 248.dp
                } else {
                    if (compact) 168.dp else 192.dp
                }
                val cardHeight = if (compact) 84.dp else 96.dp

                Box(
                    modifier = cardFocusMod
                        .width(cardWidth)
                        .height(cardHeight)
                        .mishkatFocus(id = "channel:${channel.id}")
                        .clip(shape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = if (isFirst) tokens.cardFocused else tokens.card,
                            radius = tokens.cardRadius,
                            glassIntensity = glassIntensity,
                            borderColor = if (isFirst) tokens.primary.copy(alpha = 0.45f) else tokens.glassRim,
                        )
                        .clickable { onSelectChannel(channel) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = channel.glyph,
                                    fontSize = if (isFirst) 22.sp else 18.sp,
                                )
                                if (isFirst) {
                                    Text(
                                        text = if (isArabic) "مميز" else "FEATURED",
                                        color = tokens.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isFirst) Color(0xFFDC2626) else tokens.primary.copy(alpha = 0.22f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (isFirst) "LIVE • ${channel.badge}" else channel.badge,
                                    color = if (isFirst) Color.White else tokens.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = channel.displayName(isArabic),
                                color = tokens.onSurface,
                                fontSize = if (isFirst) (if (compact) 15.sp else 17.sp) else (if (compact) 13.sp else 14.sp),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val subText = if (channel.frequency.isNotBlank()) {
                                "${channel.displaySubtitle(isArabic)} • ${channel.frequency} ${channel.polarization}"
                            } else {
                                channel.displaySubtitle(isArabic)
                            }
                            Text(
                                text = subText,
                                color = tokens.onSurfaceMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveTvPlayerOverlay(
    tokens: ThemeTokens,
    repository: LiveTvRepository,
    isArabic: Boolean,
    onClose: () -> Unit,
) {
    val state by repository.state.collectAsState()
    val context = LocalContext.current
    var showOsd by remember { mutableStateOf(true) }
    var showChannelMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val drawerFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (showChannelMenu) {
            showChannelMenu = false
            runCatching { focusRequester.requestFocus() }
        } else {
            repository.stop()
            onClose()
        }
    }

    // Auto-hide OSD banner after 5 seconds of inactivity
    LaunchedEffect(state.currentChannel, showOsd) {
        if (showOsd && !showChannelMenu) {
            delay(5000L)
            showOsd = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(showChannelMenu) {
        if (showChannelMenu) {
            delay(60)
            runCatching { drawerFocusRequester.requestFocus() }
        } else {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .mishkatFocus(id = "livetv:player_screen")
            .onPreviewKeyEvent { ev ->
                if (ev.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                if (showChannelMenu) {
                    if (ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                        showChannelMenu = false
                        runCatching { focusRequester.requestFocus() }
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                }
                when (val code = ev.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                        repository.previousChannel()
                        showOsd = true
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        repository.nextChannel()
                        showOsd = true
                        true
                    }
                    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                        val digit = code - KeyEvent.KEYCODE_0
                        repository.jumpToChannelByNumber(digit)
                        showOsd = true
                        true
                    }
                    in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
                        val digit = code - KeyEvent.KEYCODE_NUMPAD_0
                        repository.jumpToChannelByNumber(digit)
                        showOsd = true
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_INFO -> {
                        showChannelMenu = true
                        showOsd = false
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        repository.stop()
                        onClose()
                        true
                    }
                    else -> false
                }
            },
    ) {
        val activeCh = state.currentChannel
        if (activeCh?.isTuner == true) {
            // Internal Hardware Tuner View (DVB-S2 / DTV)
            AndroidView(
                factory = { ctx ->
                    TvView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        val uriStr = activeCh.tunerChannelUri
                        if (!uriStr.isNullOrBlank()) {
                            runCatching { tune(activeCh.inputId ?: "", Uri.parse(uriStr)) }
                        }
                    }
                },
                update = { tvView ->
                    val uriStr = activeCh.tunerChannelUri
                    if (!uriStr.isNullOrBlank()) {
                        runCatching { tvView.tune(activeCh.inputId ?: "", Uri.parse(uriStr)) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // HLS Stream Video Viewport using Media3 PlayerView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        useController = false
                        player = repository.getPlayer()
                        isFocusable = false
                        isFocusableInTouchMode = false
                    }
                },
                update = { playerView ->
                    playerView.player = repository.getPlayer()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Buffering / Loading Indicator
        if (state.isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .liquidGlass(tokens, tokens.card, 14.dp, 0.9f)
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isArabic) "⏳ جارٍ تحميل البث المباشر..." else "⏳ Loading Live Stream...",
                        color = tokens.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Error message if any
        if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .padding(24.dp),
                ) {
                    Text(
                        text = "⚠️ ${state.errorMessage}",
                        color = Color(0xFFF87171),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isArabic) "اضغط ▲ أو ▼ لتغيير القناة" else "Press ▲ or ▼ to zap channel",
                        color = Color.White,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // On-Screen Display (OSD) banner (top and bottom gradient scrims)
        AnimatedVisibility(
            visible = showOsd,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar OSD
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                            ),
                        )
                        .padding(horizontal = 44.dp, vertical = 24.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        state.currentChannel?.let { ch ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(ch.glyph, fontSize = 28.sp)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = ch.displayName(isArabic),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = ch.displaySubtitle(isArabic),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }

                        // Close button / Guide
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    repository.stop()
                                    onClose()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (isArabic) "✕ خروج (Back)" else "✕ Exit (Back)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Bottom bar OSD navigation hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            ),
                        )
                        .padding(horizontal = 44.dp, vertical = 20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isArabic) "▲ / ▼ تقليب القنوات • OK قائمة القنوات السريعة"
                            else "▲ / ▼ Zap Channels • OK Open Channels Menu",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        val currentCh = state.currentChannel
                        val tag = if (currentCh?.isTuner == true) "📡 DVB-S2 • TUNER" else "🔴 LIVE • HLS • 1080p"
                        Text(
                            text = tag,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Floating Quick Channel Selector Drawer on OK press
        if (showChannelMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showChannelMenu = false },
                contentAlignment = if (isArabic) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Column(
                    modifier = Modifier
                        .width(440.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isArabic) "📺 قائمة القنوات المباشرة" else "📺 Live Channels Menu",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { showChannelMenu = false }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "✕",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Category Selector in Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ChannelCategory.entries.forEach { cat ->
                            val isSelected = cat == state.activeCategory
                            Box(
                                modifier = Modifier
                                    .mishkatFocus(id = "drawer:cat:${cat.id}", radius = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) tokens.primary else Color.White.copy(alpha = 0.10f))
                                    .clickable { repository.selectCategory(cat) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = "${cat.glyph} ${if (isArabic) cat.titleAr else cat.titleEn}",
                                    color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }

                    // Channels List in Drawer
                    val channels = state.filteredChannels.ifEmpty { state.channels }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        channels.forEachIndexed { index, ch ->
                            val isPlaying = ch.id == state.currentChannel?.id
                            val isDefaultTarget = isPlaying || (state.currentChannel == null && index == 0)
                            val channelMod = if (isDefaultTarget) Modifier.focusRequester(drawerFocusRequester) else Modifier
                            Row(
                                modifier = channelMod
                                    .fillMaxWidth()
                                    .mishkatFocus(id = "drawer:ch:${ch.id}", radius = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPlaying) tokens.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
                                    .border(
                                        width = if (isPlaying) 1.5.dp else 0.5.dp,
                                        color = if (isPlaying) tokens.primary else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable {
                                        repository.playChannel(ch)
                                        showChannelMenu = false
                                        showOsd = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = ch.glyph, fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = ch.displayName(isArabic),
                                            color = if (isPlaying) tokens.primary else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = ch.displaySubtitle(isArabic),
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isPlaying) Color(0xFFDC2626) else Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = if (isPlaying) "شغال الآن" else ch.badge,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelAndTransponderManagerModal(
    tokens: ThemeTokens,
    repository: LiveTvRepository,
    isArabic: Boolean,
    onDismiss: () -> Unit,
) {
    val state by repository.state.collectAsState()
    var isAddingChannel by remember { mutableStateOf(false) }

    // Add Channel Form state
    var newChannelName by remember { mutableStateOf("") }
    var newFreq by remember { mutableStateOf("11747") }
    var newPol by remember { mutableStateOf("V") }
    var newSymbolRate by remember { mutableStateOf("27500") }
    var newStreamUrl by remember { mutableStateOf("") }
    var selectedSatellite by remember { mutableStateOf(SatelliteSource.NILESAT_7W) }
    var selectedCategory by remember { mutableStateOf(ChannelCategory.EGYPT_GENERAL) }

    val firstFocus = remember { FocusRequester() }

    BackHandler {
        if (isAddingChannel) {
            isAddingChannel = false
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        delay(60)
        runCatching { firstFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6050A14))
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            val shape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xF00A1020))
                    .border(1.dp, tokens.primary.copy(alpha = 0.5f), shape)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = if (isArabic) "⚙️ إدارة القنوات وترددات الأقمار" else "⚙️ Channel & Transponder Manager",
                            color = tokens.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(tokens.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (isArabic) "${state.channels.size} قناة مسجلة" else "${state.channels.size} Channels",
                                color = tokens.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Toggle Add Form Button
                        val addBtnShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .mishkatFocus(id = "mgr:add_btn", radius = 10.dp)
                                .focusRequester(firstFocus)
                                .clip(addBtnShape)
                                .background(if (isAddingChannel) Color(0xFFDC2626) else tokens.primary)
                                .clickable { isAddingChannel = !isAddingChannel }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = if (isAddingChannel) (if (isArabic) "✕ إلغاء الإضافة" else "✕ Cancel")
                                       else (if (isArabic) "➕ إضافة تردد / قناة جديدة" else "➕ Add New Channel"),
                                color = if (isAddingChannel) Color.White else Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .mishkatFocus(id = "mgr:close_btn", radius = 10.dp)
                                .clip(addBtnShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onDismiss() }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = if (isArabic) "إغلاق" else "Close",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                if (isAddingChannel) {
                    // Form to add custom channel/transponder
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, tokens.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = if (isArabic) "بيانات التردد والقناة الجديدة:" else "New Channel & Transponder Details:",
                            color = tokens.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        // Satellite Selector Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (isArabic) "القمر الصناعي:" else "Satellite:",
                                color = Color.White,
                                fontSize = 13.sp,
                            )
                            listOf(
                                SatelliteSource.NILESAT_7W,
                                SatelliteSource.BADR_26E,
                                SatelliteSource.ESHAIL_25E,
                                SatelliteSource.YAHSAT_52E,
                            ).forEach { sat ->
                                val sel = selectedSatellite == sat
                                Box(
                                    modifier = Modifier
                                        .mishkatFocus(id = "sat_pick:${sat.id}", radius = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) tokens.primary else Color(0x22FFFFFF))
                                        .clickable { selectedSatellite = sat }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = if (isArabic) sat.titleAr else sat.titleEn,
                                        color = if (sel) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        // Form Inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ManagerInputField(
                                id = "mgr_in:name",
                                label = if (isArabic) "اسم القناة" else "Channel Name",
                                value = newChannelName,
                                onValueChange = { newChannelName = it },
                                modifier = Modifier.weight(1.5f),
                            )
                            ManagerInputField(
                                id = "mgr_in:freq",
                                label = if (isArabic) "التردد (Freq)" else "Frequency",
                                value = newFreq,
                                onValueChange = { newFreq = it },
                                modifier = Modifier.weight(1f),
                            )
                            // Polarity toggle V / H
                            Column(modifier = Modifier.weight(0.8f)) {
                                Text(
                                    text = if (isArabic) "الاستقطاب" else "Polarity",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("V", "H").forEach { p ->
                                        val pSel = newPol == p
                                        Box(
                                            modifier = Modifier
                                                .mishkatFocus(id = "pol_pick:$p", radius = 6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (pSel) tokens.primary else Color(0x33FFFFFF))
                                                .clickable { newPol = p }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                text = p,
                                                color = if (pSel) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            )
                                        }
                                    }
                                }
                            }
                            ManagerInputField(
                                id = "mgr_in:sym",
                                label = if (isArabic) "الترميز (Symbol Rate)" else "Symbol Rate",
                                value = newSymbolRate,
                                onValueChange = { newSymbolRate = it },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Stream URL
                        ManagerInputField(
                            id = "mgr_in:url",
                            label = if (isArabic) "رابط البث الحي (HLS / m3u8 stream URL)" else "Stream URL (m3u8)",
                            value = newStreamUrl,
                            onValueChange = { newStreamUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Save Button
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .mishkatFocus(id = "mgr_in:submit", radius = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10B981))
                                .clickable {
                                    if (newChannelName.isNotBlank() && newStreamUrl.isNotBlank()) {
                                        val newChan = LiveChannel(
                                            id = "custom_${System.currentTimeMillis()}",
                                            nameAr = newChannelName,
                                            nameEn = newChannelName,
                                            category = selectedCategory.id,
                                            subtitleAr = "$newFreq $newPol $newSymbolRate",
                                            streamUrl = newStreamUrl,
                                            glyph = "📺",
                                            badge = "Custom",
                                            satellite = selectedSatellite.id,
                                            frequency = newFreq,
                                            polarization = newPol,
                                            symbolRate = newSymbolRate,
                                            sourceType = "HLS_STREAM",
                                        )
                                        repository.addCustomChannel(newChan)
                                        isAddingChannel = false
                                        newChannelName = ""
                                        newStreamUrl = ""
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = if (isArabic) "💾 حفظ القناة في الذاكرة" else "💾 Save Channel",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Channels Table
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.channels.forEachIndexed { index, channel ->
                        ChannelManagerRow(
                            index = index + 1,
                            channel = channel,
                            tokens = tokens,
                            isArabic = isArabic,
                            onMoveUp = { repository.moveChannel(channel.id, -1) },
                            onMoveDown = { repository.moveChannel(channel.id, 1) },
                            onDelete = { repository.deleteChannel(channel.id) },
                            onPlay = {
                                repository.playChannel(channel)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerInputField(
    id: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .mishkatFocus(id = id, radius = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ChannelManagerRow(
    index: Int,
    channel: LiveChannel,
    tokens: ThemeTokens,
    isArabic: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
) {
    val rowShape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .mishkatFocus(id = "mgr_row:${channel.id}", radius = 10.dp)
            .clip(rowShape)
            .background(Color(0xFF131D33))
            .border(0.8.dp, Color(0x33FFFFFF), rowShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(text = "#$index", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = channel.glyph, fontSize = 18.sp)
            Column {
                Text(
                    text = channel.displayName(isArabic),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${channel.satellite} | ${channel.frequency} ${channel.polarization} ${channel.symbolRate}",
                    color = tokens.primary,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }

        // Action Buttons Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MgrActionBtn("mgr_btn:play_${channel.id}", "▶ تشغيل", Color(0xFF10B981), onPlay)
            MgrActionBtn("mgr_btn:up_${channel.id}", "▲", Color(0x44FFFFFF), onMoveUp)
            MgrActionBtn("mgr_btn:down_${channel.id}", "▼", Color(0x44FFFFFF), onMoveDown)
            MgrActionBtn("mgr_btn:del_${channel.id}", "🗑️", Color(0x44EF4444), onDelete)
        }
    }
}

@Composable
private fun MgrActionBtn(
    id: String,
    label: String,
    bgColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .mishkatFocus(id = id, radius = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

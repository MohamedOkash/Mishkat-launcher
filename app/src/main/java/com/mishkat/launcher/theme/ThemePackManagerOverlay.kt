package com.mishkat.launcher.theme

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus

/**
 * §10 S3 + S6: Liquid Glass Theme Studio & Islamic Wallpapers / Art Gallery.
 */
@Composable
fun ThemePackManagerOverlay(
    tokens: ThemeTokens,
    themes: List<ThemeSpec>,
    selectedWallpaperId: String,
    isArabic: Boolean,
    glassIntensity: Float,
    isLightMode: Boolean = false,
    onSelectTheme: (String) -> Unit,
    onSelectWallpaper: (String) -> Unit,
    onCycleGlassIntensity: () -> Unit,
    onToggleLightMode: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val firstFocus = remember { FocusRequester() }
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
            .padding(horizontal = 44.dp, vertical = 28.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.theme_manager_title),
                            color = tokens.primary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.theme_manager_subtitle),
                            color = tokens.onSurfaceMuted,
                            fontSize = 13.sp,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val pillShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .mishkatFocus(id = "theme_mgr:light_dark", radius = 12.dp)
                                .clip(pillShape)
                                .liquidGlass(tokens, tokens.cardFocused, 12.dp, glassIntensity, tokens.primary)
                                .clickable { onToggleLightMode() }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = if (isLightMode) {
                                    if (isArabic) "☀️ الوضع الفاتح نشط" else "☀️ Light Mode"
                                } else {
                                    if (isArabic) "🌙 الوضع الداكن نشط" else "🌙 Dark Mode"
                                },
                                color = tokens.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .mishkatFocus(id = "theme_mgr:glass_level", radius = 12.dp)
                                .clip(pillShape)
                                .liquidGlass(tokens, tokens.card, 12.dp, glassIntensity)
                                .clickable { onCycleGlassIntensity() }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.theme_glass_level_fmt,
                                    (glassIntensity * 100).toInt(),
                                ),
                                color = tokens.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .mishkatFocus(id = "theme_mgr:close", radius = 12.dp)
                                .clip(pillShape)
                                .liquidGlass(tokens, tokens.card, 12.dp, glassIntensity)
                                .clickable { onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.radio_close),
                                color = tokens.onSurface,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // 1. Themes Shelf (5 Liquid Glass Islamic Themes)
            item(key = "themes_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.section_themes),
                        color = tokens.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                    ) {
                        items(
                            items = themes,
                            key = { it.id },
                            contentType = { "theme_pack" },
                        ) { spec ->
                            val selected = spec.id == tokens.id
                            val isFirst = spec.id == themes.firstOrNull()?.id
                            val previewTokens = remember(spec, isLightMode) {
                                ThemeTokens.fromSpec(spec, isLightMode = isLightMode)
                            }
                            val shape = RoundedCornerShape(16.dp)
                            val displayName = if (isArabic) spec.name else (spec.nameEn ?: spec.name)

                            Box(
                                modifier = (if (isFirst) Modifier.focusRequester(firstFocus) else Modifier)
                                    .width(215.dp)
                                    .height(132.dp)
                                    .mishkatFocus(id = "theme_pack:${spec.id}", radius = 16.dp)
                                    .clip(shape)
                                    .border(
                                        BorderStroke(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) tokens.primary else tokens.stroke,
                                        ),
                                        shape,
                                    )
                                    .clickable { onSelectTheme(spec.id) },
                            ) {
                                // Live mini wallpaper inside each theme card
                                ThemeWallpaper(
                                    tokens = previewTokens,
                                    wallpaperOverride = spec.wallpaper.pattern?.type ?: "arabesque_mandala",
                                    animate = false,
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = displayName,
                                            color = previewTokens.primary,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (selected) {
                                            Text(
                                                text = "✓",
                                                color = previewTokens.primary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(
                                            previewTokens.primary,
                                            previewTokens.accentPrayer,
                                            previewTokens.accentSleep,
                                            previewTokens.accentWeather,
                                        ).forEach { swatch ->
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(swatch)
                                                    .border(BorderStroke(1.dp, previewTokens.onSurface), CircleShape),
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Liquid Glass • ${(spec.cards.glass * 100).toInt()}%",
                                        color = previewTokens.onSurfaceMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Wallpapers & Islamic Art Gallery Shelf
            item(key = "wallpapers_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.section_wallpapers),
                        color = tokens.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                    ) {
                        items(
                            items = WALLPAPER_PRESETS,
                            key = { it.id },
                            contentType = { "wallpaper_preset" },
                        ) { preset ->
                            val selected = preset.id == selectedWallpaperId
                            val shape = RoundedCornerShape(16.dp)
                            val title = if (isArabic) preset.titleAr else preset.titleEn
                            val sub = if (isArabic) preset.subtitleAr else preset.subtitleEn

                            Box(
                                modifier = Modifier
                                    .width(235.dp)
                                    .height(140.dp)
                                    .mishkatFocus(id = "wallpaper:${preset.id}", radius = 16.dp)
                                    .clip(shape)
                                    .border(
                                        BorderStroke(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) tokens.primary else tokens.stroke,
                                        ),
                                        shape,
                                    )
                                    .clickable { onSelectWallpaper(preset.id) },
                            ) {
                                ThemeWallpaper(
                                    tokens = tokens,
                                    wallpaperOverride = preset.id,
                                    animate = false,
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(tokens.surface.copy(alpha = 0.32f))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = title,
                                            color = tokens.primary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (selected) {
                                            Text(
                                                text = "✓",
                                                color = tokens.primary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }

                                    Text(
                                        text = sub,
                                        color = tokens.onSurface,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
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

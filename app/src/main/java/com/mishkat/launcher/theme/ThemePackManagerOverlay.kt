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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
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
    autoWallpaperManager: com.mishkat.launcher.wallpaper.AutoWallpaperManager? = null,
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

                        if (autoWallpaperManager != null) {
                            val autoState by autoWallpaperManager.state.collectAsState()
                            val intervalLabel = if (isArabic) autoState.interval.labelAr else autoState.interval.labelEn
                            Box(
                                modifier = Modifier
                                    .mishkatFocus(id = "theme_mgr:auto_rotate", radius = 12.dp)
                                    .clip(pillShape)
                                    .liquidGlass(tokens, tokens.card, 12.dp, glassIntensity)
                                    .clickable {
                                        val entries = com.mishkat.launcher.wallpaper.AutoWallpaperInterval.entries
                                        val nextIdx = (autoState.interval.ordinal + 1) % entries.size
                                        autoWallpaperManager.setInterval(entries[nextIdx])
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    text = if (isArabic) "🔄 تدوير الخلفية: $intervalLabel" else "🔄 Auto-Rotate: $intervalLabel",
                                    color = tokens.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
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

            // Categorized Living Themes (Pure Themes Only - 30 Integrated Themes)
            THEME_CATEGORIES.forEach { category ->
                val categoryThemes = category.themeIds.mapNotNull { id -> themes.firstOrNull { it.id == id } }
                if (categoryThemes.isNotEmpty()) {
                    item(key = "theme_cat_${category.titleEn}") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitlePill(
                                title = if (isArabic) category.titleAr else category.titleEn,
                                tokens = tokens,
                                icon = category.icon,
                                badgeText = "${categoryThemes.size} ثيم",
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                            ) {
                                items(
                                    items = categoryThemes,
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
                                            .width(225.dp)
                                            .height(138.dp)
                                            .mishkatFocus(id = "theme_pack:${spec.id}", radius = 16.dp)
                                            .clip(shape)
                                            .border(
                                                BorderStroke(
                                                    if (selected) 2.5.dp else 1.dp,
                                                    if (selected) tokens.primary else tokens.stroke,
                                                ),
                                                shape,
                                            )
                                            .clickable {
                                                onSelectTheme(spec.id)
                                                onSelectWallpaper("auto")
                                            },
                                    ) {
                                        // Live mini wallpaper inside each theme card
                                        ThemeWallpaper(
                                            tokens = previewTokens,
                                            wallpaperOverride = spec.wallpaper.pattern?.type ?: "arabesque_mandala",
                                            animate = false,
                                        )

                                        // Glass dark gradient for crystal clear text readability
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        listOf(
                                                            Color(0x33000000),
                                                            Color(0xCC000000),
                                                        )
                                                    )
                                                )
                                                .padding(14.dp),
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
                                                    Text(
                                                        text = displayName,
                                                        color = previewTokens.primary,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    if (selected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(tokens.primary.copy(alpha = 0.25f))
                                                                .border(1.dp, tokens.primary, RoundedCornerShape(6.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                                        ) {
                                                            Text(
                                                                text = "✓ نشط",
                                                                color = tokens.primary,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                            )
                                                        }
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
                    }
                }
            }
        }
    }
}

private data class ThemeCategoryDef(
    val titleAr: String,
    val titleEn: String,
    val icon: String,
    val themeIds: List<String>,
)

private val THEME_CATEGORIES = listOf(
    ThemeCategoryDef(
        titleAr = "الثيمات الحية التفاعلية 4K",
        titleEn = "Live Dynamic Interactive Themes 4K",
        icon = "✨",
        themeIds = listOf(
            "live_winter_snow", "live_winter_rain", "live_storm_lightning",
            "live_spring_petals", "live_autumn_leaves", "live_summer_waves",
            "live_stadium_lights", "live_cinema_bokeh", "live_mishkat_lanterns",
            "live_shamsa_mandala"
        ),
    ),
    ThemeCategoryDef(
        titleAr = "فصول السنة الأربعة الحية 4K",
        titleEn = "Four Seasons Live 4K",
        icon = "🌸",
        themeIds = listOf(
            "season_spring_4k", "season_summer_4k", "season_autumn_4k", "season_winter_4k",
            "spring_blossom", "summer_sunset", "winter_snow"
        ),
    ),
    ThemeCategoryDef(
        titleAr = "سحر الطبيعة والشلالات والجبال 4K",
        titleEn = "Majestic Nature & Waterfalls 4K",
        icon = "🏔️",
        themeIds = listOf(
            "nature_mountains_4k", "nature_waterfall_4k", "nature_yosemite",
            "nature_misty_forest", "nature_rain_window", "nature_winter_avenue"
        ),
    ),
    ThemeCategoryDef(
        titleAr = "عالم الصقور والطيور الحرة 4K",
        titleEn = "Wildlife, Falcons & Birds 4K",
        icon = "🦅",
        themeIds = listOf("birds_falcon_eagle_4k", "birds_colorful_kingfisher_4k"),
    ),
    ThemeCategoryDef(
        titleAr = "الفخامة الداكنة والزجاج السائل OLED 4K",
        titleEn = "Luxury Dark, Carbon & Liquid Glass OLED 4K",
        icon = "🖤",
        themeIds = listOf(
            "dark_luxury_onyx_4k", "dark_carbon_mesh_4k", "cyber_onyx",
            "oled_amethyst", "oled_emerald", "oled_sapphire",
            "oled_titanium", "oled_obsidian_gold", "oled_crimson"
        ),
    ),
    ThemeCategoryDef(
        titleAr = "المعالم الإسلامية المقدسة والزخرفة الذهبية",
        titleEn = "Sacred Islamic Heritage & Gold Ornament",
        icon = "🕌",
        themeIds = listOf(
            "islamic_makkah_dawn", "islamic_madinah_sunset", "islamic_quran_navy",
            "kaaba_gold", "rawda_sharifa", "madinah_marble", "noor", "zomorod",
            "rimal", "layla", "andalus_arch", "fajr_sky", "damascus_mosaic",
            "turqoise_dome", "sahara_dunes", "islamic_kairouan", "islamic_isfahan", "islamic_cordoba"
        ),
    ),
    ThemeCategoryDef(
        titleAr = "عالم السينما والرياضة والأورورا 4K",
        titleEn = "Cinema, Sports & Aurora 4K",
        icon = "🎬",
        themeIds = listOf("cinema_action", "sports_arena", "aurora_borealis", "ruby_velvet"),
    ),
)

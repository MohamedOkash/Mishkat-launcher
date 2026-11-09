package com.mishkat.launcher.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import com.mishkat.launcher.theme.LocalThemeTokens
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass

@Composable
fun TvInputsQuickShelf(
    tokens: ThemeTokens,
    tvState: TvInputsUiState,
    firstFocusRequester: FocusRequester? = null,
    onSelectSource: (TvSourceItem) -> Unit,
    onOpenTvHub: () -> Unit,
) {
    val appSettings = LocalAppSettings.current
    val isArabic = appSettings.isArabic
    val compact = appSettings.compactLayout
    val glass = appSettings.glassIntensity

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tv_inputs_shelf_title),
                color = tokens.onSurface,
                fontSize = if (compact) 15.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (tvState.usbDrives.isNotEmpty()) {
                val usbShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .clip(usbShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.22f))
                        .border(BorderStroke(1.dp, Color(0xFF10B981)), usbShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (isArabic) {
                            "💾 فلاشة USB متصلة (${tvState.usbDrives.size})"
                        } else {
                            "💾 USB Connected (${tvState.usbDrives.size})"
                        },
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tvState.sources.forEachIndexed { idx, source ->
                val isDefault = source.id == tvState.defaultSourceId
                val cardW = if (compact) 186.dp else 212.dp
                val cardH = if (compact) 70.dp else 82.dp
                val shape = RoundedCornerShape(tokens.cardRadius)

                Column(
                    modifier = (if (idx == 0 && firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
                        .width(cardW)
                        .height(cardH)
                        .mishkatFocus(id = "tv_shelf:${source.id}")
                        .clip(shape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = if (isDefault) tokens.cardFocused else tokens.card,
                            radius = tokens.cardRadius,
                            glassIntensity = glass,
                            borderColor = if (isDefault) tokens.primary else tokens.glassRim,
                        )
                        .clickable { onSelectSource(source) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = source.glyph, fontSize = 15.sp)
                            Text(
                                text = source.title(isArabic),
                                color = if (isDefault) tokens.primary else tokens.onSurface,
                                fontSize = if (compact) 12.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (source.isHardwareDetected || source.isConnected) {
                                        Color(0xFF22C55E)
                                    } else {
                                        tokens.onSurfaceMuted.copy(alpha = 0.45f)
                                    },
                                ),
                        )
                    }

                    Text(
                        text = source.subtitle(isArabic),
                        color = tokens.onSurfaceMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val hubCardW = if (compact) 186.dp else 212.dp
            val hubCardH = if (compact) 70.dp else 82.dp
            val hubShape = RoundedCornerShape(tokens.cardRadius)
            Column(
                modifier = Modifier
                    .width(hubCardW)
                    .height(hubCardH)
                    .mishkatFocus(id = "tv:open_hub")
                    .clip(hubShape)
                    .liquidGlass(tokens, tokens.cardFocused, tokens.cardRadius, glass, tokens.primary)
                    .clickable { onOpenTvHub() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "📡 " + stringResource(R.string.tv_inputs_open_hub),
                    color = tokens.primary,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isArabic) "إدارة القمر الصناعي والمداخل" else "Manage Satellite & HDMI",
                    color = tokens.onSurface,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun TvInputsPanelOverlay(
    tokens: ThemeTokens,
    tvState: TvInputsUiState,
    onSelectSource: (TvSourceItem) -> Unit,
    onSetDefaultSource: (String) -> Unit,
    onToggleAutoBootLiveTv: (Boolean) -> Unit,
    onRefreshInputs: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val appSettings = LocalAppSettings.current
    val isArabic = appSettings.isArabic
    val glass = appSettings.glassIntensity
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
            .background(tokens.surface.copy(alpha = 0.96f))
            .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.tv_inputs_panel_title),
                        color = tokens.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.tv_inputs_panel_subtitle),
                        color = tokens.onSurfaceMuted,
                        fontSize = 13.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val pillShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "tv_panel:refresh", radius = 14.dp)
                            .clip(pillShape)
                            .liquidGlass(tokens, tokens.card, 14.dp, glass)
                            .clickable { onRefreshInputs() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = if (isArabic) "فحص المداخل والـ USB 🔄" else "Scan Inputs & USB 🔄",
                            color = tokens.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "tv_panel:close", radius = 14.dp)
                            .clip(pillShape)
                            .liquidGlass(tokens, tokens.card, 14.dp, glass)
                            .clickable { onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.radio_close),
                            color = tokens.onSurface,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // Auto-start Receiver on Boot toggle bar + Remote Color Shortcuts bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val barShape = RoundedCornerShape(tokens.cardRadius)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .mishkatFocus(id = "tv_panel:auto_boot")
                        .clip(barShape)
                        .liquidGlass(tokens, tokens.card, tokens.cardRadius, glass)
                        .clickable { onToggleAutoBootLiveTv(!tvState.autoStartLiveTvOnBoot) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isArabic) {
                                "فتح الريسيفر / البث التلفزيوني تلقائياً عند تشغيل الشاشة"
                            } else {
                                "Auto-open Satellite Receiver / Live TV on TV boot"
                            },
                            color = tokens.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isArabic) {
                                "مفيد لشاشات الأندرويد المزودة بريسيفر داخلي (أندرويد 7 فما فوق)"
                            } else {
                                "Ideal for Android 7.0+ Smart TVs with built-in satellite tuner"
                            },
                            color = tokens.onSurfaceMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        text = if (tvState.autoStartLiveTvOnBoot) {
                            stringResource(R.string.settings_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        color = if (tvState.autoStartLiveTvOnBoot) tokens.primary else tokens.onSurfaceMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Remote Color Keys Legend
                Row(
                    modifier = Modifier
                        .clip(barShape)
                        .liquidGlass(tokens, tokens.card, tokens.cardRadius, glass)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorKeyHint("🔴", if (isArabic) "الإذاعة" else "Radio")
                    ColorKeyHint("🟢", if (isArabic) "الريسيفر" else "TV/Input")
                    ColorKeyHint("🟡", if (isArabic) "المباريات" else "Sports")
                    ColorKeyHint("🔵", if (isArabic) "الثيمات" else "Themes")
                }
            }

            // Sources List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                itemsIndexed(
                    items = tvState.sources,
                    key = { _, s -> s.id },
                    contentType = { _, _ -> "tv_source_row" },
                ) { idx, source ->
                    val isDefault = source.id == tvState.defaultSourceId
                    val rowShape = RoundedCornerShape(tokens.cardRadius)
                    Row(
                        modifier = (if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier)
                            .fillMaxWidth()
                            .mishkatFocus(id = "tv_row:${source.id}")
                            .clip(rowShape)
                            .liquidGlass(
                                tokens = tokens,
                                baseColor = if (isDefault) tokens.cardFocused else tokens.card,
                                radius = tokens.cardRadius,
                                glassIntensity = glass,
                                borderColor = if (isDefault) tokens.primary else tokens.glassRim,
                            )
                            .clickable { onSelectSource(source) }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = source.glyph, fontSize = 24.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = source.title(isArabic),
                                        color = tokens.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (isDefault) {
                                        Text(
                                            text = if (isArabic) "★ المصدر الافتراضي (زر TV)" else "★ Default (TV Key)",
                                            color = tokens.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                Text(
                                    text = source.subtitle(isArabic),
                                    color = tokens.onSurfaceMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!isDefault && source.category != TvSourceCategory.USB_STORAGE) {
                                val defShape = RoundedCornerShape(10.dp)
                                Box(
                                    modifier = Modifier
                                        .mishkatFocus(id = "tv_def:${source.id}", radius = 10.dp)
                                        .clip(defShape)
                                        .background(tokens.surface.copy(alpha = 0.55f))
                                        .border(BorderStroke(1.dp, tokens.stroke), defShape)
                                        .clickable { onSetDefaultSource(source.id) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = if (isArabic) "تعيين كافتراضي" else "Set Default",
                                        color = tokens.onSurfaceMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }

                            val openShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .clip(openShape)
                                    .background(tokens.primary.copy(alpha = 0.2f))
                                    .border(BorderStroke(1.dp, tokens.primary), openShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = if (isArabic) "فتح الآن ◄" else "Open ◄",
                                    color = tokens.primary,
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
}

@Composable
private fun ColorKeyHint(dot: String, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = dot, fontSize = 12.sp)
        Text(
            text = label,
            color = LocalThemeTokens.current.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

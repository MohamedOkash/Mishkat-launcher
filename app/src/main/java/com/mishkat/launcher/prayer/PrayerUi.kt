package com.mishkat.launcher.prayer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.LocalAppSettings
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass
import java.util.Locale

@Composable
fun PrayerAndAdhkarRow(
    tokens: ThemeTokens,
    prayerState: PrayerUiState,
    use24Hour: Boolean = false,
    firstFocusRequester: FocusRequester? = null,
    onOpenCityPicker: () -> Unit,
    onIncrementTasbih: () -> Unit,
    onOpenAdhkarPanel: () -> Unit,
) {
    val appSettings = LocalAppSettings.current
    val isArabic = appSettings.isArabic
    val compact = appSettings.compactLayout
    val glass = appSettings.glassIntensity
    val cardHeight = if (compact) 94.dp else 114.dp
    val shape = RoundedCornerShape(tokens.cardRadius)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1) Prayer Times Card (5 obligatory times + next prayer countdown + city name)
        Column(
            modifier = (if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
                .weight(1.35f)
                .height(cardHeight)
                .mishkatFocus(id = "prayer:card")
                .clip(shape)
                .liquidGlass(tokens, tokens.card, tokens.cardRadius, glass)
                .clickable { onOpenCityPicker() }
                .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.prayer_change_city,
                        prayerState.city.displayName(isArabic),
                    ),
                    color = tokens.primary,
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )

                val next = prayerState.nextPrayer
                if (next != null) {
                    Text(
                        text = stringResource(
                            R.string.prayer_next_label,
                            prayerKindLabel(next.kind),
                            PrayerEngine.formatCountdown(next.remainingMs),
                        ),
                        color = tokens.onSurface,
                        fontSize = if (compact) 12.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            val today = prayerState.todayTimes
            if (today != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    today.obligatoryPrayers.forEach { (kind, timeMs) ->
                        val isNext = prayerState.nextPrayer?.kind == kind
                        val pillShape = RoundedCornerShape(10.dp)
                        Column(
                            modifier = Modifier
                                .clip(pillShape)
                                .liquidGlass(
                                    tokens = tokens,
                                    baseColor = if (isNext) tokens.cardFocused else tokens.card,
                                    radius = 10.dp,
                                    glassIntensity = glass,
                                    borderColor = if (isNext) tokens.primary else tokens.glassRim,
                                )
                                .padding(horizontal = if (compact) 9.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                text = prayerKindLabel(kind),
                                color = if (isNext) tokens.primary else tokens.onSurfaceMuted,
                                fontSize = if (compact) 11.sp else 12.sp,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                            )
                            Text(
                                text = PrayerEngine.formatLocalTime(
                                    epochMs = timeMs,
                                    tzId = prayerState.city.tz,
                                    use24Hour = use24Hour,
                                ),
                                color = tokens.onSurface,
                                fontSize = if (compact) 11.sp else 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // 2) Interactive Adhkar / Tasbih Card (OK button increments counter; NO nested clickables!)
        val dhikr = prayerState.currentDhikr
        Column(
            modifier = Modifier
                .weight(0.95f)
                .height(cardHeight)
                .mishkatFocus(id = "adhkar:card")
                .clip(shape)
                .liquidGlass(tokens, tokens.card, tokens.cardRadius, glass)
                .clickable { onIncrementTasbih() }
                .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = adhkarWindowLabel(prayerState.activeWindow),
                    color = tokens.primary,
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (dhikr != null) {
                    Text(
                        text = stringResource(
                            R.string.adhkar_tasbih_counter,
                            prayerState.currentDhikrCount,
                            dhikr.targetCount,
                        ),
                        color = tokens.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (dhikr != null) {
                AnimatedContent(
                    targetState = dhikr,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(350))
                    },
                    label = "dhikr_rotation"
                ) { targetDhikr ->
                    Text(
                        text = targetDhikr.displayText(isArabic),
                        color = tokens.onSurface,
                        fontSize = if (compact) 12.sp else 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // 3) Dedicated Top-Level Card for All Adhkar & Hijri Occasions (Sibling card, smooth D-Pad focus!)
        val allAdhkarLabel = stringResource(R.string.adhkar_open_all)
        Column(
            modifier = Modifier
                .width(if (compact) 190.dp else 220.dp)
                .height(cardHeight)
                .mishkatFocus(id = "adhkar:open_all")
                .clip(shape)
                .liquidGlass(tokens, tokens.cardFocused, tokens.cardRadius, glass, tokens.primary)
                .clickable { onOpenAdhkarPanel() }
                .padding(horizontal = 13.dp, vertical = if (compact) 8.dp else 11.dp)
                .semantics { contentDescription = allAdhkarLabel },
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "📿 $allAdhkarLabel",
                color = tokens.primary,
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = prayerState.upcomingHijriEvent?.label(isArabic)
                    ?: (if (isArabic) prayerState.hijriDateAr else prayerState.hijriDateEn),
                color = tokens.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * §4 + §9 + §10 DoD: AdhanOverlay displayed on top of any screen inside the launcher when prayer time arrives.
 */
@Composable
fun AdhanOverlay(
    tokens: ThemeTokens,
    kind: PrayerKind,
    cityName: String,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    BackHandler(onBack = onDismiss)

    val dismissFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(3) {
            withFrameNanos { }
            if (runCatching { dismissFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.surface.copy(alpha = 0.96f))
            .padding(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        val cardShape = RoundedCornerShape(tokens.cardRadius)
        Column(
            modifier = Modifier
                .width(680.dp)
                .clip(cardShape)
                .background(tokens.accentPrayer)
                .border(BorderStroke(2.dp, tokens.primary), cardShape)
                .padding(horizontal = 44.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = cityName,
                color = tokens.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = stringResource(R.string.adhan_overlay_title, prayerKindLabel(kind)),
                color = tokens.onSurface,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.adhan_overlay_subtitle),
                color = tokens.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.adhan_overlay_dua),
                color = tokens.onSurfaceMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            val btnShape = RoundedCornerShape(16.dp)
            val dismissLabel = stringResource(R.string.adhan_overlay_dismiss)
            Box(
                modifier = Modifier
                    .focusRequester(dismissFocus)
                    .mishkatFocus(id = "adhan:dismiss", radius = 16.dp)
                    .focusable()
                    .clip(btnShape)
                    .background(tokens.cardFocused)
                    .border(BorderStroke(1.dp, tokens.primary), btnShape)
                    .clickable { onDismiss() }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp &&
                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                            onDismiss()
                            true
                        } else false
                    }
                    .padding(horizontal = 28.dp, vertical = 12.dp)
                    .semantics { contentDescription = dismissLabel },
            ) {
                Text(
                    text = dismissLabel,
                    color = tokens.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun CityPickerOverlay(
    tokens: ThemeTokens,
    cities: List<CitySpec>,
    selectedCityId: String,
    selectedMuezzin: MuezzinVoice = MuezzinVoice.MAKKAH,
    isAdhanTesting: Boolean = false,
    onSelectCity: (CitySpec) -> Unit,
    onSelectMuezzin: (MuezzinVoice) -> Unit = {},
    onTestAdhan: () -> Unit = {},
    onPreviewAdhan: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val isArabic = remember { Locale.getDefault().language != "en" }
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
            .padding(horizontal = 56.dp, vertical = 36.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.prayer_pick_city_title),
                    color = tokens.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val previewLabel = stringResource(R.string.prayer_test_adhan)
                    val pillShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "city_picker:preview_adhan", radius = 14.dp)
                            .clip(pillShape)
                            .background(tokens.cardFocused)
                            .border(BorderStroke(1.dp, tokens.primary), pillShape)
                            .clickable { onPreviewAdhan() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = previewLabel },
                    ) {
                        Text(
                            text = previewLabel,
                            color = tokens.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    val closeLabel = stringResource(R.string.radio_close)
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "city_picker:close", radius = 14.dp)
                            .clip(pillShape)
                            .background(tokens.card)
                            .border(BorderStroke(1.dp, tokens.stroke), pillShape)
                            .clickable { onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = closeLabel },
                    ) {
                        Text(
                            text = closeLabel,
                            color = tokens.onSurface,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // Level 2: Muezzin Selector Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isArabic) "🎙 اختيار صوت المؤذن (أذان بشري نقي 100%)" else "🎙 Select Muezzin Voice (Pure Human Audio)",
                        color = tokens.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "prayer:test_adhan_toggle", radius = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAdhanTesting) tokens.primary else tokens.cardFocused)
                            .border(BorderStroke(1.dp, tokens.primary), RoundedCornerShape(12.dp))
                            .clickable { onTestAdhan() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (isAdhanTesting) (if (isArabic) "⏹ إيقاف الأذان" else "⏹ Stop Adhan")
                                   else (if (isArabic) "▶ سماع وتجربة الأذان" else "▶ Test Adhan Audio"),
                            color = if (isAdhanTesting) tokens.surface else tokens.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MuezzinVoice.entries.forEach { m ->
                        val isSelected = m == selectedMuezzin
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .mishkatFocus(id = "muezzin:${m.id}", radius = 12.dp)
                                .clip(shape)
                                .background(if (isSelected) tokens.cardFocused else tokens.card)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) tokens.primary else tokens.stroke),
                                    shape,
                                )
                                .clickable { onSelectMuezzin(m) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = m.displayName(isArabic),
                                color = if (isSelected) tokens.primary else tokens.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                items(
                    items = cities,
                    key = { it.id },
                    contentType = { "city" },
                ) { city ->
                    val selected = city.id == selectedCityId
                    val isFirst = city.id == cities.firstOrNull()?.id
                    val shape = RoundedCornerShape(tokens.cardRadius)
                    Row(
                        modifier = (if (isFirst) Modifier.focusRequester(firstFocus) else Modifier)
                            .fillMaxWidth()
                            .mishkatFocus(id = "city:${city.id}")
                            .clip(shape)
                            .background(if (selected) tokens.cardFocused else tokens.card)
                            .border(
                                BorderStroke(1.dp, if (selected) tokens.primary else tokens.stroke),
                                shape,
                            )
                            .clickable {
                                onSelectCity(city)
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${city.displayName(isArabic)} (${city.country})",
                            color = tokens.onSurface,
                            fontSize = 16.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            text = city.tz,
                            color = if (selected) tokens.primary else tokens.onSurfaceMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // §3 / §12 Mandatory CC-BY 4.0 GeoNames attribution
            Text(
                text = stringResource(R.string.geonames_attribution),
                color = tokens.onSurfaceMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun AdhkarPanelOverlay(
    tokens: ThemeTokens,
    initialWindow: AdhkarWindow,
    onLoadWindow: (AdhkarWindow) -> List<DhikrSpec>,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val isArabic = remember { Locale.getDefault().language != "en" }
    var selectedWindow by remember { mutableStateOf(initialWindow) }
    val items = remember(selectedWindow) { onLoadWindow(selectedWindow) }
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
            .padding(horizontal = 56.dp, vertical = 36.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdhkarWindow.entries.forEachIndexed { idx, w ->
                        val active = w == selectedWindow
                        val shape = RoundedCornerShape(14.dp)
                        val label = adhkarWindowLabel(w)
                        Box(
                            modifier = (if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .mishkatFocus(id = "adhkar_tab:${w.name}", radius = 14.dp)
                                .clip(shape)
                                .background(if (active) tokens.cardFocused else tokens.card)
                                .border(
                                    BorderStroke(1.dp, if (active) tokens.primary else tokens.stroke),
                                    shape,
                                )
                                .clickable { selectedWindow = w }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = label,
                                color = if (active) tokens.primary else tokens.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }

                val closeLabel = stringResource(R.string.radio_close)
                val pillShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "adhkar_panel:close", radius = 14.dp)
                        .clip(pillShape)
                        .background(tokens.card)
                        .border(BorderStroke(1.dp, tokens.stroke), pillShape)
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics { contentDescription = closeLabel },
                ) {
                    Text(
                        text = closeLabel,
                        color = tokens.onSurface,
                        fontSize = 14.sp,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(
                    items = items,
                    key = { it.id },
                    contentType = { "dhikr_row" },
                ) { d ->
                    var count by remember(d.id) { mutableStateOf(0) }
                    val shape = RoundedCornerShape(tokens.cardRadius)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .mishkatFocus(id = "dhikr_item:${d.id}")
                            .clip(shape)
                            .background(tokens.card)
                            .border(BorderStroke(1.dp, tokens.stroke), shape)
                            .clickable {
                                count = if (count + 1 >= d.targetCount) 0 else count + 1
                            }
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = d.displayText(isArabic),
                            color = tokens.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.adhkar_tasbih_counter, count, d.targetCount),
                            color = tokens.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun prayerKindLabel(kind: PrayerKind): String = when (kind) {
    PrayerKind.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerKind.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerKind.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerKind.ASR -> stringResource(R.string.prayer_asr)
    PrayerKind.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerKind.ISHA -> stringResource(R.string.prayer_isha)
}

@Composable
fun adhkarWindowLabel(window: AdhkarWindow): String = when (window) {
    AdhkarWindow.MORNING -> stringResource(R.string.adhkar_window_morning)
    AdhkarWindow.EVENING -> stringResource(R.string.adhkar_window_evening)
    AdhkarWindow.AFTER_PRAYER -> stringResource(R.string.adhkar_window_after_prayer)
    AdhkarWindow.SLEEP -> stringResource(R.string.adhkar_window_sleep)
}

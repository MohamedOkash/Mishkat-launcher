package com.mishkat.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.LocalAppSettings
import com.mishkat.launcher.prayer.PrayerEngine
import com.mishkat.launcher.prayer.PrayerUiState
import com.mishkat.launcher.prayer.adhkarWindowLabel
import com.mishkat.launcher.prayer.prayerKindLabel
import com.mishkat.launcher.radio.RadioUiState
import com.mishkat.launcher.sports.SportsUiState
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass
import com.mishkat.launcher.weather.WeatherSnapshot
import com.mishkat.launcher.weather.weatherCodeGlyph
import com.mishkat.launcher.weather.weatherCodeLabel
import kotlinx.coroutines.delay

/**
 * §4 + R23: Compact Liquid Glass Billboard Carousel.
 * Rotates automatically through 4 live cards (Quran Live Stream, Next Prayer & Dhikr, Weather & Hijri, Featured Match)
 * and pauses immediately when an overlay is open or lifecycle is not RESUMED (R23).
 */
@Composable
fun BillboardCarousel(
    tokens: ThemeTokens,
    isVisible: Boolean,
    isArabic: Boolean,
    radioState: RadioUiState,
    prayerState: PrayerUiState,
    weatherSnapshot: WeatherSnapshot?,
    sportsState: SportsUiState = SportsUiState(),
    firstFocusRequester: FocusRequester? = null,
    onActionSlide: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var slideIndex by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val appSettings = LocalAppSettings.current
    val compact = appSettings.compactLayout

    // R23: pause auto-advance whenever not visible or activity not resumed.
    LaunchedEffect(isVisible, lifecycleOwner) {
        if (!isVisible) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(7_000L)
                slideIndex = (slideIndex + 1) % SLIDE_COUNT
            }
        }
    }

    val shape = RoundedCornerShape(tokens.cardRadius)

    Box(
        modifier = (if (firstFocusRequester != null) modifier.focusRequester(firstFocusRequester) else modifier)
            .fillMaxWidth()
            .height(if (compact) 74.dp else 90.dp)
            .mishkatFocus(id = "billboard:hero")
            .clip(shape)
            .liquidGlass(
                tokens = tokens,
                baseColor = tokens.card,
                radius = tokens.cardRadius,
                glassIntensity = appSettings.glassIntensity,
                borderColor = tokens.primary.copy(alpha = 0.55f),
            )
            .clickable { onActionSlide(slideIndex) }
            .padding(horizontal = 18.dp, vertical = if (compact) 10.dp else 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                when (slideIndex) {
                    0 -> {
                        val reciter = radioState.currentStation?.displayName(isArabic)
                            ?: stringResource(R.string.section_radio)
                        Text(
                            text = stringResource(R.string.billboard_quran_title, reciter),
                            color = tokens.primary,
                            fontSize = if (compact) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.billboard_quran_subtitle),
                            color = tokens.onSurface,
                            fontSize = if (compact) 12.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    1 -> {
                        val next = prayerState.nextPrayer
                        val header = if (next != null) {
                            stringResource(
                                R.string.prayer_next_label,
                                prayerKindLabel(next.kind),
                                PrayerEngine.formatCountdown(next.remainingMs),
                            )
                        } else {
                            adhkarWindowLabel(prayerState.activeWindow)
                        }
                        Text(
                            text = header,
                            color = tokens.primary,
                            fontSize = if (compact) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = prayerState.currentDhikr?.displayText(isArabic).orEmpty(),
                            color = tokens.onSurface,
                            fontSize = if (compact) 12.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    2 -> {
                        val city = prayerState.city.displayName(isArabic)
                        val weatherTitle = if (weatherSnapshot != null) {
                            "$city • ${weatherSnapshot.currentTempC}°C ${weatherCodeGlyph(weatherSnapshot.currentWeatherCode)} ${weatherCodeLabel(weatherSnapshot.currentWeatherCode)}"
                        } else {
                            "$city • ${stringResource(R.string.section_weather)}"
                        }
                        Text(
                            text = weatherTitle,
                            color = tokens.primary,
                            fontSize = if (compact) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = if (isArabic) prayerState.hijriDateAr else prayerState.hijriDateEn,
                            color = tokens.onSurface,
                            fontSize = if (compact) 12.sp else 13.sp,
                            maxLines = 1,
                        )
                    }
                    else -> {
                        val match = sportsState.featuredMatch
                        if (match != null) {
                            val winner = match.winnerName(isArabic)
                            val scoreOrVs = if (match.isUpcoming) "VS" else "${match.homeScore} - ${match.awayScore}"
                            Text(
                                text = "⚽ ${match.leagueName(isArabic)} • ${match.homeName(isArabic)} $scoreOrVs ${match.awayName(isArabic)}",
                                color = tokens.primary,
                                fontSize = if (compact) 15.sp else 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (winner != null) {
                                    stringResource(R.string.sports_winner_fmt, winner)
                                } else {
                                    match.statusText
                                },
                                color = tokens.onSurface,
                                fontSize = if (compact) 12.sp else 13.sp,
                                maxLines = 1,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sports_panel_title),
                                color = tokens.primary,
                                fontSize = if (compact) 15.sp else 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Slide indicator dots (visual only so they never trap D-Pad focus)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(SLIDE_COUNT) { idx ->
                    val active = idx == slideIndex
                    Box(
                        modifier = Modifier
                            .size(if (active) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (active) tokens.primary else tokens.onSurfaceMuted),
                    )
                }
            }
        }
    }
}

private const val SLIDE_COUNT = 4

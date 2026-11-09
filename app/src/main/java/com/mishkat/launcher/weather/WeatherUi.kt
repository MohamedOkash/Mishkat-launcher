package com.mishkat.launcher.weather

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.theme.ThemeTokens

@Composable
fun WeatherPanelOverlay(
    tokens: ThemeTokens,
    cityName: String,
    snapshot: WeatherSnapshot?,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(4) {
            withFrameNanos { }
            if (runCatching { closeFocus.requestFocus() }.isSuccess) return@LaunchedEffect
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${stringResource(R.string.section_weather)} — $cityName",
                        color = tokens.primary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (snapshot != null) {
                        Text(
                            text = "${snapshot.currentTempC}°C • ${weatherCodeLabel(snapshot.currentWeatherCode)}",
                            color = tokens.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val pillShape = RoundedCornerShape(14.dp)
                    val refreshLabel = stringResource(R.string.radio_refresh)
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "weather:refresh", radius = 14.dp)
                            .clip(pillShape)
                            .background(tokens.card)
                            .border(BorderStroke(1.dp, tokens.stroke), pillShape)
                            .clickable { onRefresh() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = refreshLabel },
                    ) {
                        Text(text = refreshLabel, color = tokens.onSurface, fontSize = 14.sp)
                    }

                    val closeLabel = stringResource(R.string.radio_close)
                    Box(
                        modifier = Modifier
                            .focusRequester(closeFocus)
                            .mishkatFocus(id = "weather:close", radius = 14.dp)
                            .clip(pillShape)
                            .background(tokens.cardFocused)
                            .border(BorderStroke(1.dp, tokens.primary), pillShape)
                            .clickable { onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = closeLabel },
                    ) {
                        Text(
                            text = closeLabel,
                            color = tokens.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (snapshot == null || snapshot.days.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.weather_offline_notice),
                        color = tokens.onSurfaceMuted,
                        fontSize = 18.sp,
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(
                        items = snapshot.days,
                        key = { it.dateIso },
                        contentType = { "day_forecast" },
                    ) { day ->
                        val shape = RoundedCornerShape(tokens.cardRadius)
                        Column(
                            modifier = Modifier
                                .width(172.dp)
                                .height(190.dp)
                                .mishkatFocus(id = "weather_day:${day.dateIso}")
                                .clip(shape)
                                .background(tokens.accentWeather)
                                .border(BorderStroke(1.dp, tokens.stroke), shape)
                                .clickable { }
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = day.dateIso.takeLast(5),
                                color = tokens.primary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )

                            Text(
                                text = weatherCodeGlyph(day.weatherCode),
                                color = tokens.onSurface,
                                fontSize = 30.sp,
                            )

                            Text(
                                text = weatherCodeLabel(day.weatherCode),
                                color = tokens.onSurface,
                                fontSize = 13.sp,
                            )

                            Text(
                                text = "${day.tempMaxC}° / ${day.tempMinC}°",
                                color = tokens.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun weatherCodeGlyph(code: Int): String = when (code) {
    0, 1 -> "☀"
    2, 3 -> "⛅"
    in 45..48 -> "🌫"
    in 51..67, in 80..82 -> "🌧"
    in 71..77 -> "❄"
    in 95..99 -> "⛈"
    else -> "☀"
}

@Composable
fun weatherCodeLabel(code: Int): String = when (code) {
    0, 1 -> stringResource(R.string.weather_clear)
    2, 3 -> stringResource(R.string.weather_cloudy)
    in 45..48 -> stringResource(R.string.weather_fog)
    in 51..67, in 80..82 -> stringResource(R.string.weather_rain)
    else -> stringResource(R.string.weather_clear)
}

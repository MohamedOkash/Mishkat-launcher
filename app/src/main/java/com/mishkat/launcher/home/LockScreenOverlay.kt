package com.mishkat.launcher.home

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.AppSettings
import com.mishkat.launcher.i18n.LocaleFormatter
import com.mishkat.launcher.prayer.PrayerEngine
import com.mishkat.launcher.prayer.PrayerUiState
import com.mishkat.launcher.prayer.prayerKindLabel
import com.mishkat.launcher.radio.RadioUiState
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.ThemeWallpaper
import com.mishkat.launcher.theme.liquidGlass
import com.mishkat.launcher.weather.WeatherSnapshot
import com.mishkat.launcher.weather.weatherCodeGlyph
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date

private val AMBIENT_VERSES_AR = listOf(
    "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",
    "﴿ اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ مَثَلُ نُورِهِ كَمِشْكَاةٍ فِيهَا مِصْبَاحٌ ﴾",
    "﴿ وَقُلْ رَبِّ زِدْنِي عِلْمًا ﴾",
    "﴿ رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً ﴾",
    "﴿ إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ ﴾",
)

private val AMBIENT_VERSES_EN = listOf(
    "\"Verily, in the remembrance of Allah do hearts find rest.\" (13:28)",
    "\"Allah is the Light of the heavens and the earth; the likeness of His light is as a niche (Mishkat)...\" (24:35)",
    "\"My Lord, increase me in knowledge.\" (20:114)",
    "\"Our Lord, give us in this world that which is good and in the Hereafter that which is good.\" (2:201)",
    "\"Indeed, Allah and His angels send blessings upon the Prophet.\" (33:56)",
)

/**
 * S6 + S8: Ultra-Dimmed Night Lock Screen & Ambient Mode.
 * - Lowers hardware/window brightness (`screenBrightness = 0.06f`) AND applies a deep night scrim
 *   so the room stays restful and dark while Quran radio plays or while the TV is idle.
 */
@Composable
fun LockScreenOverlay(
    tokens: ThemeTokens,
    appSettings: AppSettings,
    prayerState: PrayerUiState,
    radioState: RadioUiState,
    weatherSnapshot: WeatherSnapshot?,
    onToggleRadioPlayPause: () -> Unit,
    onNextStation: () -> Unit,
    onCycleWallpaper: () -> Unit,
    onUnlock: () -> Unit,
) {
    BackHandler(onBack = onUnlock)

    val context = LocalContext.current
    val unlockFocus = remember { FocusRequester() }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var verseIndex by remember { mutableIntStateOf(0) }
    // 0 = Ultra Dim (8% brightness, 0.58 extra dim), 1 = Deep Black Night (4% brightness, 0.68 extra dim), 2 = Soft Dim (18% brightness, 0.42 extra dim)
    var dimMode by remember { mutableIntStateOf(0) }

    val extraDimAlpha = when (dimMode) {
        1 -> 0.68f
        2 -> 0.42f
        else -> 0.58f
    }
    val winBrightness = when (dimMode) {
        1 -> 0.03f
        2 -> 0.18f
        else -> 0.07f
    }

    // Apply real window brightness reduction while LockScreenOverlay is displayed, restore on exit.
    DisposableEffect(winBrightness) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = winBrightness
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    LaunchedEffect(Unit) {
        repeat(4) {
            withFrameNanos { }
            if (runCatching { unlockFocus.requestFocus() }.isSuccess) return@repeat
        }
        while (true) {
            delay(10_000L)
            nowMillis = System.currentTimeMillis()
            verseIndex = (verseIndex + 1) % AMBIENT_VERSES_AR.size
        }
    }

    val isArabic = appSettings.isArabic
    val timeText = remember(nowMillis, appSettings.use24Hour, appSettings.language) {
        val pattern = if (appSettings.use24Hour) "HH:mm" else "hh:mm a"
        val raw = SimpleDateFormat(pattern, appSettings.locale).format(Date(nowMillis))
        LocaleFormatter.localizeDigits(raw, isArabic)
    }

    val hijriDate = if (isArabic) prayerState.hijriDateAr else prayerState.hijriDateEn
    val verse = if (isArabic) AMBIENT_VERSES_AR[verseIndex] else AMBIENT_VERSES_EN[verseIndex]
    val stationTitle = radioState.currentStation?.displayName(isArabic)
        ?: stringResource(R.string.section_radio)

    val softGold = tokens.primary.copy(alpha = 0.78f)
    val softWhite = Color(0xFFE2E8F0).copy(alpha = 0.76f)
    val softMuted = Color(0xFF94A3B8).copy(alpha = 0.68f)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ThemeWallpaper(
            tokens = tokens,
            wallpaperOverride = appSettings.wallpaperStyle,
            weatherCode = weatherSnapshot?.currentWeatherCode,
            animate = false,
            extraDimAlpha = extraDimAlpha,
        )

        val panelShape = RoundedCornerShape(26.dp)
        Column(
            modifier = Modifier
                .width(640.dp)
                .clip(panelShape)
                .liquidGlass(
                    tokens = tokens,
                    baseColor = Color(0xFF04070E).copy(alpha = 0.82f),
                    radius = 26.dp,
                    glassIntensity = 0.55f,
                    borderColor = tokens.primary.copy(alpha = 0.28f),
                )
                .padding(horizontal = 34.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Top badge: City + Weather + Hijri Date
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = prayerState.city.displayName(isArabic),
                    color = softGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (weatherSnapshot != null) {
                    Text(
                        text = "• ${weatherCodeGlyph(weatherSnapshot.currentWeatherCode)} ${weatherSnapshot.currentTempC}°C",
                        color = softWhite,
                        fontSize = 14.sp,
                    )
                }
                if (hijriDate.isNotBlank()) {
                    Text(
                        text = "• $hijriDate",
                        color = softMuted,
                        fontSize = 14.sp,
                    )
                }
            }

            // Soft Night Clock (non-glaring)
            Text(
                text = timeText,
                color = softWhite,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
            )

            // Rotating Quranic Ayah
            Text(
                text = verse,
                color = softGold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Next Prayer Pill
            val next = prayerState.nextPrayer
            if (next != null) {
                val pillShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .liquidGlass(tokens, Color(0xFF09101D).copy(alpha = 0.85f), 14.dp, 0.5f)
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.prayer_next_label,
                            prayerKindLabel(next.kind),
                            PrayerEngine.formatCountdown(next.remainingMs),
                        ),
                        color = softWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Live Quran Radio Control inside Lock Screen
            val radioShape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(radioShape)
                    .liquidGlass(tokens, Color(0xFF080D18).copy(alpha = 0.86f), 16.dp, 0.5f)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stationTitle,
                        color = softWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (radioState.isPlaying) {
                            stringResource(R.string.radio_now_playing)
                        } else {
                            stringResource(R.string.radio_idle)
                        },
                        color = softGold,
                        fontSize = 11.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val btnShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "lock:radio_toggle", radius = 10.dp)
                            .clip(btnShape)
                            .liquidGlass(tokens, Color(0xFF10192B), 10.dp, 0.55f)
                            .clickable { onToggleRadioPlayPause() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (radioState.isPlaying || radioState.isBuffering) {
                                stringResource(R.string.radio_pause)
                            } else {
                                stringResource(R.string.radio_play)
                            },
                            color = softGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "lock:radio_next", radius = 10.dp)
                            .clip(btnShape)
                            .liquidGlass(tokens, Color(0xFF10192B), 10.dp, 0.55f)
                            .clickable { onNextStation() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "⏭",
                            color = softWhite,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // Bottom Action Row: Unlock + Dimness Level + Change Backdrop
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val btnShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .focusRequester(unlockFocus)
                        .mishkatFocus(id = "lock:unlock", radius = 14.dp)
                        .clip(btnShape)
                        .liquidGlass(
                            tokens = tokens,
                            baseColor = Color(0xFF111A2E),
                            radius = 14.dp,
                            glassIntensity = 0.55f,
                            borderColor = softGold,
                        )
                        .clickable { onUnlock() }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.lock_screen_unlock),
                        color = softGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val dimLabel = when (dimMode) {
                    1 -> if (isArabic) "🌙 إضاءة: ليلي معتم 5%" else "🌙 Dim: Deep Night 5%"
                    2 -> if (isArabic) "🌙 إضاءة: هادئة 20%" else "🌙 Dim: Soft 20%"
                    else -> if (isArabic) "🌙 إضاءة: خافتة جداً 10%" else "🌙 Dim: Ultra Low 10%"
                }
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "lock:dim_mode", radius = 14.dp)
                        .clip(btnShape)
                        .liquidGlass(tokens, Color(0xFF0C1322), 14.dp, 0.5f)
                        .clickable { dimMode = (dimMode + 1) % 3 }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = dimLabel,
                        color = softWhite,
                        fontSize = 12.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "lock:wallpaper", radius = 14.dp)
                        .clip(btnShape)
                        .liquidGlass(tokens, Color(0xFF0C1322), 14.dp, 0.5f)
                        .clickable { onCycleWallpaper() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.lock_screen_change_bg),
                        color = softMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

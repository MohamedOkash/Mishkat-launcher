package com.mishkat.launcher.theme

import android.view.KeyEvent
import com.mishkat.launcher.input.MishkatAction
import com.mishkat.launcher.input.SmartTvReceiverAdapter
import com.mishkat.launcher.prayer.PrayerEngine
import com.mishkat.launcher.remote.QrCodeEncoder
import com.mishkat.launcher.sports.SportsRepository
import com.mishkat.launcher.system.CrashLoopPolicy
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeAndCrashPolicyTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseAllBuiltinThemeAssets() {
        val dirCandidates = listOf(
            File("src/main/assets/themes"),
            File("app/src/main/assets/themes"),
        )
        val dir = dirCandidates.first { it.isDirectory }
        val files = dir.listFiles { f -> f.name.endsWith(".json") }.orEmpty().sortedBy { it.name }
        assertEquals(
            listOf(
                "andalus_arch.json",
                "aurora_borealis.json",
                "cinema_action.json",
                "cyber_onyx.json",
                "damascus_mosaic.json",
                "fajr_sky.json",
                "kaaba_gold.json",
                "layla.json",
                "madinah_marble.json",
                "noor.json",
                "rawda_sharifa.json",
                "rimal.json",
                "ruby_velvet.json",
                "sahara_dunes.json",
                "sports_arena.json",
                "spring_blossom.json",
                "summer_sunset.json",
                "turqoise_dome.json",
                "winter_snow.json",
                "zomorod.json",
            ),
            files.map { it.name },
        )

        for (f in files) {
            val spec = json.decodeFromString(ThemeSpec.serializer(), f.readText())
            assertTrue(spec.builtin)
            val tokens = ThemeTokens.fromSpec(spec)
            assertEquals(spec.id, tokens.id)
            assertFalse(tokens.isLightMode)
            assertTrue(tokens.focusScale >= 1.0f)

            val lightTokens = tokens.withLightMode(true)
            assertTrue(lightTokens.isLightMode)
            assertEquals(spec.id, lightTokens.id)
        }
    }

    @Test
    fun weatherSyncWallpaperResolvesByWeatherCode() {
        assertEquals("winter_snow", resolveWeatherSyncedWallpaper(weatherCode = 71))
        assertEquals("winter_rain", resolveWeatherSyncedWallpaper(weatherCode = 61))
        assertEquals("storm_lightning", resolveWeatherSyncedWallpaper(weatherCode = 95))
        assertEquals("spring_petals", resolveWeatherSyncedWallpaper(weatherCode = 2))
        assertEquals("summer_waves", resolveWeatherSyncedWallpaper(weatherCode = 0))
        assertEquals("autumn_leaves", resolveWeatherSyncedWallpaper(weatherCode = 45))
        assertEquals("winter_snow", resolveWeatherSyncedWallpaper(weatherCode = null))
    }

    @Test
    fun allRealPhotographicWallpapersExistOnDisk() {
        val dirCandidates = listOf(
            File("src/main/assets/wallpapers"),
            File("app/src/main/assets/wallpapers"),
        )
        val dir = dirCandidates.first { it.isDirectory }
        for (preset in WALLPAPER_PRESETS) {
            val f = File(dir, preset.assetFile)
            assertTrue("Missing real photo ${preset.assetFile}", f.isFile && f.length() > 10_000L)
        }
    }

    @Test
    fun smartTvReceiverAndQrEncoderWorkCorrectly() {
        assertEquals(MishkatAction.TvLive, SmartTvReceiverAdapter.translate(KeyEvent.KEYCODE_TV, null))
        assertEquals(MishkatAction.TvInput, SmartTvReceiverAdapter.translate(KeyEvent.KEYCODE_TV_INPUT, null))
        assertEquals(MishkatAction.ColorRed, SmartTvReceiverAdapter.translate(KeyEvent.KEYCODE_PROG_RED, null))
        assertEquals(MishkatAction.ColorGreen, SmartTvReceiverAdapter.translate(KeyEvent.KEYCODE_PROG_GREEN, null))

        val hijriEv = PrayerEngine.computeUpcomingHijriEventFromMonthDay(monthZeroBased = 7, dayOfMonth = 20)
        assertEquals("شهر رمضان المبارك", hijriEv.titleAr)
        assertEquals(11, hijriEv.daysRemaining)

        val qr = QrCodeEncoder.encodeVersion3L("http://192.168.1.6:8787")
        assertEquals(29, qr.size)
        assertTrue(qr[0][0]) // Top-left finder corner
        assertTrue(qr[0][28]) // Top-right finder corner
        assertTrue(qr[28][0]) // Bottom-left finder corner
    }

    @Test
    fun parseEspnScoreboardExtractsWinnerAndStatus() {
        val sampleJson = """
            {
              "events": [
                {
                  "id": "991",
                  "date": "2026-04-20T19:00Z",
                  "status": {
                    "type": {
                      "state": "post",
                      "shortDetail": "FT"
                    }
                  },
                  "competitions": [
                    {
                      "competitors": [
                        {
                          "homeAway": "home",
                          "score": "3",
                          "team": { "displayName": "Al Ahly" }
                        },
                        {
                          "homeAway": "away",
                          "score": "1",
                          "team": { "displayName": "Mamelodi Sundowns" }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val parsed = SportsRepository.parseEspnScoreboard(
            rawJson = sampleJson,
            leagueCode = "caf.champions",
            leagueAr = "دوري أبطال أفريقيا",
            leagueEn = "CAF Champions League",
        )
        assertEquals(1, parsed.size)
        val match = parsed.first()
        assertEquals("post", match.state)
        assertTrue(match.isFinished)
        assertEquals("الأهلي المصري", match.homeAr)
        assertEquals(3, match.homeScore)
        assertEquals(1, match.awayScore)
        assertEquals("الأهلي المصري", match.winnerName(isArabic = true))
        assertEquals("Al Ahly", match.winnerName(isArabic = false))
    }

    @Test
    fun crashLoopPolicyTriggersAtThreeWithinWindow() {
        val t0 = 100_000L
        assertFalse(CrashLoopPolicy.isLooping(listOf(t0, t0 + 10_000), t0 + 20_000))
        assertTrue(CrashLoopPolicy.isLooping(listOf(t0, t0 + 10_000, t0 + 20_000), t0 + 25_000))
        // Older than 60s window does not count.
        assertFalse(CrashLoopPolicy.isLooping(listOf(t0, t0 + 1_000, t0 + 2_000), t0 + 70_000))
    }
}

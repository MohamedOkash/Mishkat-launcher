package com.mishkat.launcher.prayer

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * R19: Golden tests for offline prayer times in Cairo (EGYPTIAN), Makkah (UMM_AL_QURA),
 * and Jakarta (MUSLIM_WORLD_LEAGUE) on known dates, plus Adhkar window resolution anchored to prayer times.
 */
class PrayerEngineGoldenTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun hm(epochMs: Long, tzId: String): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(tzId)
        }
        return sdf.format(Date(epochMs))
    }

    @Test
    fun goldenPrayerTimesCairoMakkahJakarta() {
        // 1) Cairo (30.0444, 31.2357, Africa/Cairo, EGYPTIAN) on 2026-03-21 (Vernal Equinox)
        val cairo = CitySpec("eg_cairo", "القاهرة", "Cairo", "EG", 30.0444, 31.2357, "Africa/Cairo")
        val cairoTimes = PrayerEngine.computeForDate(cairo, "EGYPTIAN", 2026, 3, 21)
        assertEquals("04:31", hm(cairoTimes.fajrMs, cairo.tz))
        assertEquals("05:58", hm(cairoTimes.sunriseMs, cairo.tz))
        assertEquals("12:03", hm(cairoTimes.dhuhrMs, cairo.tz))
        assertEquals("15:30", hm(cairoTimes.asrMs, cairo.tz))
        assertEquals("18:07", hm(cairoTimes.maghribMs, cairo.tz))
        assertEquals("19:25", hm(cairoTimes.ishaMs, cairo.tz))

        // 2) Makkah (21.3891, 39.8579, Asia/Riyadh, UMM_AL_QURA) on 2026-03-21
        val makkah = CitySpec("sa_makkah", "مكة المكرمة", "Makkah", "SA", 21.3891, 39.8579, "Asia/Riyadh")
        val makkahTimes = PrayerEngine.computeForDate(makkah, "UMM_AL_QURA", 2026, 3, 21)
        assertEquals("05:08", hm(makkahTimes.fajrMs, makkah.tz))
        assertEquals("06:24", hm(makkahTimes.sunriseMs, makkah.tz))
        assertEquals("12:28", hm(makkahTimes.dhuhrMs, makkah.tz))
        assertEquals("15:53", hm(makkahTimes.asrMs, makkah.tz))
        assertEquals("18:32", hm(makkahTimes.maghribMs, makkah.tz))
        assertEquals("20:02", hm(makkahTimes.ishaMs, makkah.tz))

        // 3) Jakarta (-6.2088, 106.8456, Asia/Jakarta, MUSLIM_WORLD_LEAGUE) on 2026-03-21
        val jakarta = CitySpec("id_jakarta", "جاكرتا", "Jakarta", "ID", -6.2088, 106.8456, "Asia/Jakarta")
        val jakartaTimes = PrayerEngine.computeForDate(jakarta, "MUSLIM_WORLD_LEAGUE", 2026, 3, 21)
        assertEquals("04:48", hm(jakartaTimes.fajrMs, jakarta.tz))
        assertEquals("05:57", hm(jakartaTimes.sunriseMs, jakarta.tz))
        assertEquals("12:01", hm(jakartaTimes.dhuhrMs, jakarta.tz))
        assertEquals("15:10", hm(jakartaTimes.asrMs, jakarta.tz))
        assertEquals("18:03", hm(jakartaTimes.maghribMs, jakarta.tz))
        assertEquals("19:08", hm(jakartaTimes.ishaMs, jakarta.tz))
    }

    @Test
    fun adhkarWindowFollowsPrayerTimesAndSleepTimer() {
        val cairo = CitySpec("eg_cairo", "القاهرة", "Cairo", "EG", 30.0444, 31.2357, "Africa/Cairo")
        val t = PrayerEngine.computeForDate(cairo, "EGYPTIAN", 2026, 3, 21)

        // 5 minutes after Fajr -> AFTER_PRAYER window ([Fajr+2m .. Fajr+35m])
        assertEquals(
            AdhkarWindow.AFTER_PRAYER,
            PrayerEngine.resolveAdhkarWindow(t, t.fajrMs + 5 * 60_000L, sleepTimerActive = false),
        )

        // 45 minutes after Fajr (still before Sunrise+15m) -> MORNING window
        assertEquals(
            AdhkarWindow.MORNING,
            PrayerEngine.resolveAdhkarWindow(t, t.fajrMs + 45 * 60_000L, sleepTimerActive = false),
        )

        // 10 minutes after Dhuhr -> AFTER_PRAYER window
        assertEquals(
            AdhkarWindow.AFTER_PRAYER,
            PrayerEngine.resolveAdhkarWindow(t, t.dhuhrMs + 10 * 60_000L, sleepTimerActive = false),
        )

        // 50 minutes after Asr -> EVENING window
        assertEquals(
            AdhkarWindow.EVENING,
            PrayerEngine.resolveAdhkarWindow(t, t.asrMs + 50 * 60_000L, sleepTimerActive = false),
        )

        // Anytime SleepTimer is active -> SLEEP window overrides all
        assertEquals(
            AdhkarWindow.SLEEP,
            PrayerEngine.resolveAdhkarWindow(t, t.dhuhrMs + 10 * 60_000L, sleepTimerActive = true),
        )
    }

    @Test
    fun offlinePrayerAssetsParseCleanly() {
        val baseDir = listOf(File("src/main/assets/prayer"), File("app/src/main/assets/prayer"))
            .first { it.isDirectory }

        val methods = json.decodeFromString(
            MapSerializer(String.serializer(), String.serializer()),
            File(baseDir, "country_methods.json").readText(),
        )
        assertEquals("EGYPTIAN", methods["EG"])
        assertEquals("UMM_AL_QURA", methods["SA"])
        assertEquals("KARACHI", methods["PK"])
        assertEquals("NORTH_AMERICA", methods["US"])

        val cities = json.decodeFromString(
            ListSerializer(CitySpec.serializer()),
            File(baseDir, "cities_pack.json").readText(),
        )
        assertTrue(cities.size >= 30)

        val adhkar = json.decodeFromString(
            ListSerializer(DhikrSpec.serializer()),
            File(baseDir, "adhkar.json").readText(),
        )
        assertTrue(adhkar.size >= 10)
        assertEquals(AdhkarWindow.entries.toSet(), adhkar.map { it.window }.toSet())
    }
}

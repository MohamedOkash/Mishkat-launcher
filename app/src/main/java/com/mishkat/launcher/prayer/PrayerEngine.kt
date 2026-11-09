package com.mishkat.launcher.prayer

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone as IcuTimeZone
import android.icu.util.ULocale
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * §9 + R19: Pure offline prayer calculation + Adhkar window resolution.
 * Adhkar windows are anchored to actual prayer times, NOT device wall-clock hours:
 * - [AdhkarWindow.SLEEP]: when sleepTimerActive is true.
 * - [AdhkarWindow.AFTER_PRAYER]: [prayerTime + 2 min .. prayerTime + 35 min] for any of the 5 obligatory prayers.
 * - [AdhkarWindow.MORNING]: [Fajr .. Sunrise + 15 min].
 * - [AdhkarWindow.EVENING]: [Asr .. 24:00 local midnight].
 */
object PrayerEngine {

    private const val AFTER_PRAYER_START_OFFSET_MS = 2 * 60_000L
    private const val AFTER_PRAYER_END_OFFSET_MS = 35 * 60_000L
    private const val MORNING_AFTER_SUNRISE_MS = 15 * 60_000L

    fun parametersForMethod(methodId: String): CalculationParameters {
        val method = when (methodId.uppercase(Locale.US)) {
            "EGYPTIAN" -> CalculationMethod.EGYPTIAN
            "UMM_AL_QURA" -> CalculationMethod.UMM_AL_QURA
            "DUBAI" -> CalculationMethod.DUBAI
            "KUWAIT" -> CalculationMethod.KUWAIT
            "QATAR" -> CalculationMethod.QATAR
            "KARACHI" -> CalculationMethod.KARACHI
            "NORTH_AMERICA", "ISNA" -> CalculationMethod.NORTH_AMERICA
            "SINGAPORE" -> CalculationMethod.SINGAPORE
            "MOON_SIGHTING_COMMITTEE" -> CalculationMethod.MOON_SIGHTING_COMMITTEE
            else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        }
        return method.parameters.apply { madhab = Madhab.SHAFI }
    }

    /** Computes the 6 times for the local calendar date (year, month 1..12, day 1..31) in [city]. */
    fun computeForDate(
        city: CitySpec,
        methodId: String,
        year: Int,
        month: Int,
        day: Int,
    ): DayPrayerTimes {
        val coords = Coordinates(city.lat, city.lon)
        val dateComponents = DateComponents(year, month, day)
        val params = parametersForMethod(methodId)
        val pt = PrayerTimes(coords, dateComponents, params)
        return DayPrayerTimes(
            fajrMs = pt.fajr.time,
            sunriseMs = pt.sunrise.time,
            dhuhrMs = pt.dhuhr.time,
            asrMs = pt.asr.time,
            maghribMs = pt.maghrib.time,
            ishaMs = pt.isha.time,
        )
    }

    fun computeForEpoch(city: CitySpec, methodId: String, nowEpochMs: Long): DayPrayerTimes {
        val tz = TimeZone.getTimeZone(city.tz)
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowEpochMs }
        return computeForDate(
            city = city,
            methodId = methodId,
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun nextPrayer(
        city: CitySpec,
        methodId: String,
        today: DayPrayerTimes,
        nowEpochMs: Long,
    ): NextPrayerInfo {
        for ((kind, t) in today.obligatoryPrayers) {
            if (t > nowEpochMs) {
                return NextPrayerInfo(
                    kind = kind,
                    targetEpochMs = t,
                    remainingMs = (t - nowEpochMs).coerceAtLeast(0L),
                )
            }
        }
        // After Isha: next prayer is tomorrow's Fajr.
        val tz = TimeZone.getTimeZone(city.tz)
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = nowEpochMs
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val tomorrow = computeForDate(
            city = city,
            methodId = methodId,
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        )
        return NextPrayerInfo(
            kind = PrayerKind.FAJR,
            targetEpochMs = tomorrow.fajrMs,
            remainingMs = (tomorrow.fajrMs - nowEpochMs).coerceAtLeast(0L),
        )
    }

    /**
     * Resolves the active Adhkar window strictly from prayer times + sleep timer state (§9).
     */
    fun resolveAdhkarWindow(
        today: DayPrayerTimes,
        nowEpochMs: Long,
        sleepTimerActive: Boolean,
        endOfDayEpochMs: Long = Long.MAX_VALUE,
    ): AdhkarWindow {
        if (sleepTimerActive) return AdhkarWindow.SLEEP

        // 1) Post-prayer tasbih window: [prayer + 2 min .. prayer + 35 min]
        for ((_, prayerMs) in today.obligatoryPrayers) {
            val start = prayerMs + AFTER_PRAYER_START_OFFSET_MS
            val end = prayerMs + AFTER_PRAYER_END_OFFSET_MS
            if (nowEpochMs in start..end) return AdhkarWindow.AFTER_PRAYER
        }

        // 2) Morning adhkar: [Fajr .. Sunrise + 15 min]
        if (nowEpochMs in today.fajrMs..(today.sunriseMs + MORNING_AFTER_SUNRISE_MS)) {
            return AdhkarWindow.MORNING
        }

        // 3) Evening adhkar: [Asr .. 24:00]
        if (nowEpochMs in today.asrMs..endOfDayEpochMs) {
            return AdhkarWindow.EVENING
        }

        // Outside specific windows (e.g. between Sunrise+15m and Dhuhr): default to Morning before Asr, Sleep before Fajr.
        return if (nowEpochMs < today.fajrMs) AdhkarWindow.SLEEP else AdhkarWindow.MORNING
    }

    fun formatLocalTime(epochMs: Long, tzId: String, use24Hour: Boolean = false): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(tzId)
        }
        return sdf.format(Date(epochMs))
    }

    fun formatCountdown(remainingMs: Long): String {
        val totalSec = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    }

    /** Formats the Hijri date using Android ICU IslamicCalendar (§1). */
    fun formatHijriDate(nowEpochMs: Long, tzId: String, isArabic: Boolean): String = runCatching {
        val cal = IslamicCalendar(IcuTimeZone.getTimeZone(tzId), ULocale.forLocale(Locale.getDefault())).apply {
            timeInMillis = nowEpochMs
        }
        val day = cal.get(IslamicCalendar.DAY_OF_MONTH)
        val monthIdx = cal.get(IslamicCalendar.MONTH).coerceIn(0, 11)
        val year = cal.get(IslamicCalendar.YEAR)
        val monthName = if (isArabic) HIJRI_MONTHS_AR[monthIdx] else HIJRI_MONTHS_EN[monthIdx]
        if (isArabic) "$day $monthName $year هـ" else "$day $monthName $year AH"
    }.getOrDefault("")

    private val HIJRI_MONTHS_AR = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )

    private val HIJRI_MONTHS_EN = arrayOf(
        "Muharram", "Safar", "Rabi' I", "Rabi' II",
        "Jumada I", "Jumada II", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah",
    )

    /**
     * S8: Computes the nearest upcoming Islamic event (Ramadan, Eid al-Fitr, Day of Arafah,
     * Eid al-Adha, Ashura, Mawlid, or White Days 13-15) from Hijri month (0..11) and day (1..30).
     * Pure function so it can be unit-tested on the JVM without ICU stubs.
     */
    fun computeUpcomingHijriEventFromMonthDay(monthZeroBased: Int, dayOfMonth: Int): HijriEventInfo {
        // Major annual Hijri milestones: (month 0..11, day 1..30, titleAr, titleEn)
        data class Milestone(val m: Int, val d: Int, val ar: String, val en: String)
        val milestones = listOf(
            Milestone(0, 1, "رأس السنة الهجرية", "Islamic New Year"),
            Milestone(0, 10, "يوم عاشوراء", "Day of Ashura"),
            Milestone(2, 12, "ذكرى المولد النبوي", "Mawlid al-Nabi"),
            Milestone(6, 27, "ليلة الإسراء والمعراج", "Isra & Mi'raj"),
            Milestone(8, 1, "شهر رمضان المبارك", "Blessed Ramadan"),
            Milestone(8, 27, "ليلة القدر المباركة", "Laylat al-Qadr"),
            Milestone(9, 1, "عيد الفطر المبارك", "Eid al-Fitr"),
            Milestone(11, 9, "يوم عرفة", "Day of Arafah"),
            Milestone(11, 10, "عيد الأضحى المبارك", "Eid al-Adha"),
        )
        val currentDayOfYear = monthZeroBased * 30 + dayOfMonth
        var best: Milestone = milestones.first()
        var bestDiff = Int.MAX_VALUE
        for (ms in milestones) {
            val targetDayOfYear = ms.m * 30 + ms.d
            val diff = if (targetDayOfYear >= currentDayOfYear) {
                targetDayOfYear - currentDayOfYear
            } else {
                (354 - currentDayOfYear) + targetDayOfYear
            }
            if (diff < bestDiff) {
                bestDiff = diff
                best = ms
            }
        }
        // Also check if White Days (13, 14, 15 of current Hijri month) are today or within 3 days
        if (dayOfMonth in 13..15 && bestDiff > 0) {
            return HijriEventInfo(
                titleAr = "صيام الأيام البيض ($dayOfMonth ${HIJRI_MONTHS_AR[monthZeroBased.coerceIn(0, 11)]})",
                titleEn = "White Days Fasting ($dayOfMonth ${HIJRI_MONTHS_EN[monthZeroBased.coerceIn(0, 11)]})",
                daysRemaining = 0,
                isWhiteDays = true,
            )
        }
        return HijriEventInfo(
            titleAr = best.ar,
            titleEn = best.en,
            daysRemaining = bestDiff,
        )
    }

    fun computeUpcomingHijriEvent(nowEpochMs: Long, tzId: String): HijriEventInfo = runCatching {
        val cal = IslamicCalendar(IcuTimeZone.getTimeZone(tzId), ULocale.forLocale(Locale.getDefault())).apply {
            timeInMillis = nowEpochMs
        }
        val day = cal.get(IslamicCalendar.DAY_OF_MONTH).coerceIn(1, 30)
        val monthIdx = cal.get(IslamicCalendar.MONTH).coerceIn(0, 11)
        computeUpcomingHijriEventFromMonthDay(monthIdx, day)
    }.getOrElse {
        HijriEventInfo("شهر رمضان المبارك", "Blessed Ramadan", 30)
    }
}

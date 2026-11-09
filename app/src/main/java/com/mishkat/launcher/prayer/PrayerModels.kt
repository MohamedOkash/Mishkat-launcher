package com.mishkat.launcher.prayer

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CitySpec(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val tz: String,
) {
    fun displayName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn.ifBlank { nameAr }
}

enum class PrayerKind {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

@Immutable
data class DayPrayerTimes(
    val fajrMs: Long,
    val sunriseMs: Long,
    val dhuhrMs: Long,
    val asrMs: Long,
    val maghribMs: Long,
    val ishaMs: Long,
) {
    fun timeOf(kind: PrayerKind): Long = when (kind) {
        PrayerKind.FAJR -> fajrMs
        PrayerKind.SUNRISE -> sunriseMs
        PrayerKind.DHUHR -> dhuhrMs
        PrayerKind.ASR -> asrMs
        PrayerKind.MAGHRIB -> maghribMs
        PrayerKind.ISHA -> ishaMs
    }

    val obligatoryPrayers: List<Pair<PrayerKind, Long>>
        get() = listOf(
            PrayerKind.FAJR to fajrMs,
            PrayerKind.DHUHR to dhuhrMs,
            PrayerKind.ASR to asrMs,
            PrayerKind.MAGHRIB to maghribMs,
            PrayerKind.ISHA to ishaMs,
        )
}

@Immutable
data class NextPrayerInfo(
    val kind: PrayerKind,
    val targetEpochMs: Long,
    val remainingMs: Long,
)

@Immutable
data class HijriEventInfo(
    val titleAr: String,
    val titleEn: String,
    val daysRemaining: Int,
    val isWhiteDays: Boolean = false,
) {
    fun label(isArabic: Boolean): String {
        val title = if (isArabic) titleAr else titleEn
        return if (daysRemaining <= 0) {
            if (isArabic) "🌙 اليوم: $title" else "🌙 Today: $title"
        } else {
            if (isArabic) "🌙 $title بعد $daysRemaining يوم" else "🌙 $title in $daysRemaining d"
        }
    }
}

enum class AdhkarWindow {
    MORNING,
    EVENING,
    AFTER_PRAYER,
    SLEEP,
}

@Immutable
@Serializable
data class DhikrSpec(
    val id: String,
    val window: AdhkarWindow,
    val textAr: String,
    val textEn: String,
    val targetCount: Int = 1,
) {
    fun displayText(isArabic: Boolean): String = if (isArabic) textAr else textEn.ifBlank { textAr }
}

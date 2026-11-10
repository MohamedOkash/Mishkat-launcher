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

@Serializable
enum class MuezzinVoice(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val streamUrl: String,
) {
    MAKKAH(
        "makkah",
        "أذان الحرم المكي الشريف",
        "Makkah Al-Haram Adhan",
        "https://www.islamcan.com/audio/adhan/azan1.mp3",
    ),
    ABDULBASIT(
        "abdulbasit",
        "أذان الشيخ عبد الباسط عبد الصمد",
        "Sheikh Abdulbasit Abdussamad",
        "https://www.islamcan.com/audio/adhan/azan2.mp3",
    ),
    ALAFASY(
        "alafasy",
        "أذان الشيخ مشاري راشد العفاسي",
        "Sheikh Mishary Rashid Alafasy",
        "https://www.islamcan.com/audio/adhan/azan3.mp3",
    ),
    CAIRO(
        "cairo",
        "أذان إذاعة القرآن الكريم - القاهرة",
        "Cairo Quran Radio Adhan",
        "https://www.islamcan.com/audio/adhan/azan14.mp3",
    );

    fun displayName(isArabic: Boolean): String = if (isArabic) titleAr else titleEn

    companion object {
        fun fromId(id: String): MuezzinVoice =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MAKKAH
    }
}

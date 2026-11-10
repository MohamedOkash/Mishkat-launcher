package com.mishkat.launcher.livetv

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class ChannelCategory(val id: String, val titleAr: String, val titleEn: String, val glyph: String) {
    ISLAMIC_QURAN("islamic_quran", "القرآن والقنوات الإسلامية", "Quran & Islamic", "🕋"),
    KIDS_EDUCATION("kids_education", "التعليم والأطفال", "Kids & Education", "👶"),
    EGYPT_GENERAL("egypt_general", "القنوات العامة المصرية", "Egyptian Channels", "🇪🇬"),
    DRAMA_SERIES("drama_series", "الدراما والمسلسلات", "Drama & Series", "🎭"),
    CINEMA_MOVIES("cinema_movies", "السينما والأفلام والمنوعات", "Cinema & Movies", "🍿"),
    SPORTS_COOKING("sports_cooking", "الرياضة والطبخ", "Sports & Cooking", "⚽"),
    NEWS_DOC("news_doc", "الأخبار والوثائقيات", "News & Documentaries", "📰"),
    ALL("all", "كل القنوات", "All Channels", "✦");

    companion object {
        fun fromId(id: String): ChannelCategory = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ISLAMIC_QURAN
    }
}

@Serializable
enum class SatelliteSource(val id: String, val titleAr: String, val titleEn: String, val glyph: String) {
    ALL("all", "كل الأقمار", "All Satellites", "🌐"),
    NILESAT_7W("nilesat_7w", "نايل سات 7°W", "Nilesat 7°W", "🛰"),
    BADR_26E("badr_26e", "عرب سات / بدر 26°E", "Badr/Arabsat 26°E", "📡"),
    ESHAIL_25E("eshail_25e", "سهيل سات 25.5°E", "Es'hailSat 25.5°E", "⭐"),
    YAHSAT_52E("yahsat_52e", "ياه سات 52.5°E", "Yahsat 52.5°E", "✨"),
    INTERNAL_TUNER("internal_tuner", "الريسيفر الداخلي (DVB-S2)", "Internal Tuner", "📺");

    companion object {
        fun fromId(id: String): SatelliteSource = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
    }
}

@Serializable
@Immutable
data class LiveChannel(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val category: String,
    val subtitleAr: String,
    val streamUrl: String,
    val glyph: String = "📺",
    val badge: String = "HD",
    val satellite: String = "nilesat_7w",
    val frequency: String = "",
    val polarization: String = "V",
    val symbolRate: String = "27500",
    val sourceType: String = "HLS_STREAM", // "HLS_STREAM" or "INTERNAL_TUNER"
    val tunerChannelUri: String? = null,
    val inputId: String? = null,
) {
    fun displayName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
    fun displaySubtitle(isArabic: Boolean): String = if (isArabic) subtitleAr else nameEn
    val isTuner: Boolean get() = sourceType.equals("INTERNAL_TUNER", ignoreCase = true)
}

@Immutable
data class LiveTvUiState(
    val channels: List<LiveChannel> = emptyList(),
    val currentChannel: LiveChannel? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val activeCategory: ChannelCategory = ChannelCategory.ISLAMIC_QURAN,
    val activeSatellite: SatelliteSource = SatelliteSource.ALL,
    val searchQuery: String = "",
    val lastPlayedChannelId: String? = null,
    val isSyncingCloud: Boolean = false,
) {
    val filteredChannels: List<LiveChannel>
        get() = channels.filter { ch ->
            val matchCategory = (activeCategory == ChannelCategory.ALL) || ch.category.equals(activeCategory.id, ignoreCase = true)
            val matchSatellite = (activeSatellite == SatelliteSource.ALL) || ch.satellite.equals(activeSatellite.id, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() || ch.nameAr.contains(searchQuery, ignoreCase = true) || ch.nameEn.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSatellite && matchSearch
        }
}

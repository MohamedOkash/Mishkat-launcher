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
) {
    fun displayName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
    fun displaySubtitle(isArabic: Boolean): String = if (isArabic) subtitleAr else nameEn
}

@Immutable
data class LiveTvUiState(
    val channels: List<LiveChannel> = emptyList(),
    val currentChannel: LiveChannel? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val activeCategory: ChannelCategory = ChannelCategory.ISLAMIC_QURAN,
) {
    val filteredChannels: List<LiveChannel>
        get() = if (activeCategory == ChannelCategory.ALL) channels
        else channels.filter { it.category.equals(activeCategory.id, ignoreCase = true) }
}

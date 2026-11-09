package com.mishkat.launcher.sports

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class MatchItem(
    val id: String,
    val leagueCode: String,
    val leagueAr: String,
    val leagueEn: String,
    val homeAr: String,
    val homeEn: String,
    val awayAr: String,
    val awayEn: String,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    /** "pre" = upcoming, "in" = live, "post" = finished */
    val state: String = "pre",
    val dateIso: String = "",
    val statusText: String = "",
) {
    val isLive: Boolean
        get() = state == "in"

    val isFinished: Boolean
        get() = state == "post"

    val isUpcoming: Boolean
        get() = state == "pre"

    fun leagueName(isArabic: Boolean): String = if (isArabic) leagueAr else leagueEn
    fun homeName(isArabic: Boolean): String = if (isArabic) homeAr else homeEn
    fun awayName(isArabic: Boolean): String = if (isArabic) awayAr else awayEn

    /** Returns the winning team name when the match is finished and not a draw, or null otherwise. */
    fun winnerName(isArabic: Boolean): String? {
        if (!isFinished || homeScore == awayScore) return null
        return if (homeScore > awayScore) homeName(isArabic) else awayName(isArabic)
    }

    fun involvesTeam(teamKey: String): Boolean {
        if (teamKey.isBlank()) return true
        return homeEn.contains(teamKey, ignoreCase = true) ||
            awayEn.contains(teamKey, ignoreCase = true) ||
            homeAr.contains(teamKey, ignoreCase = true) ||
            awayAr.contains(teamKey, ignoreCase = true)
    }
}

@Immutable
data class FavoriteTeamOption(
    val keyEn: String,
    val nameAr: String,
    val nameEn: String,
) {
    fun displayName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
}

val FAVORITE_TEAMS_PRESETS = listOf(
    FavoriteTeamOption("", "كل الفرق", "All Teams"),
    FavoriteTeamOption("Al Ahly", "الأهلي المصري", "Al Ahly SC"),
    FavoriteTeamOption("Zamalek", "الزمالك", "Zamalek SC"),
    FavoriteTeamOption("Al Hilal", "الهلال السعودي", "Al Hilal"),
    FavoriteTeamOption("Al Nassr", "النصر السعودي", "Al Nassr"),
    FavoriteTeamOption("Al Ittihad", "الاتحاد السعودي", "Al Ittihad"),
    FavoriteTeamOption("Real Madrid", "ريال مدريد", "Real Madrid"),
    FavoriteTeamOption("Barcelona", "برشلونة", "Barcelona"),
    FavoriteTeamOption("Liverpool", "ليفربول", "Liverpool"),
    FavoriteTeamOption("Manchester City", "مانشستر سيتي", "Manchester City"),
    FavoriteTeamOption("Arsenal", "آرسنال", "Arsenal"),
    FavoriteTeamOption("Bayern Munich", "بايرن ميونخ", "Bayern Munich"),
    FavoriteTeamOption("Inter Milan", "إنتر ميلان", "Inter Milan"),
    FavoriteTeamOption("Paris Saint-Germain", "باريس سان جيرمان", "PSG"),
)

@Immutable
data class SportsUiState(
    val matches: List<MatchItem> = emptyList(),
    val favoriteTeamKey: String = "",
    val isRefreshing: Boolean = false,
) {
    val featuredMatch: MatchItem?
        get() {
            if (favoriteTeamKey.isNotBlank()) {
                matches.firstOrNull { it.involvesTeam(favoriteTeamKey) }?.let { return it }
            }
            return matches.firstOrNull { it.isLive }
                ?: matches.firstOrNull { it.isUpcoming }
                ?: matches.firstOrNull()
        }
}

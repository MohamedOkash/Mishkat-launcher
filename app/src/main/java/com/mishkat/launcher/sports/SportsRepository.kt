package com.mishkat.launcher.sports

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
internal data class EspnScoreboardDto(
    val events: List<EspnEventDto> = emptyList(),
)

@Serializable
internal data class EspnEventDto(
    val id: String = "",
    val date: String = "",
    val competitions: List<EspnCompetitionDto> = emptyList(),
    val status: EspnStatusDto = EspnStatusDto(),
)

@Serializable
internal data class EspnCompetitionDto(
    val competitors: List<EspnCompetitorDto> = emptyList(),
)

@Serializable
internal data class EspnCompetitorDto(
    val homeAway: String = "home",
    val score: String = "0",
    val team: EspnTeamDto = EspnTeamDto(),
)

@Serializable
internal data class EspnTeamDto(
    val displayName: String = "",
)

@Serializable
internal data class EspnStatusDto(
    val type: EspnStatusTypeDto = EspnStatusTypeDto(),
)

@Serializable
internal data class EspnStatusTypeDto(
    val state: String = "pre",
    val shortDetail: String = "",
)

/**
 * S7: Offline-first + Live HTTPS Sports & Match Center Repository.
 * - Loads bundled `assets/sports/matches_snapshot.json` immediately on startup (R3).
 * - Fetches live/upcoming/finished scores from ESPN public HTTPS scoreboard across 7 major Arab & European leagues.
 */
class SportsRepository(
    private val context: Context,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(SportsUiState())
    val state: StateFlow<SportsUiState> = _state.asStateFlow()

    fun start() {
        scope.launch(Dispatchers.IO) {
            val data = runCatching { prefs.data.first() }.getOrNull()
            val favTeam = data?.get(KEY_FAV_TEAM).orEmpty()
            val cachedJson = data?.get(KEY_CACHED_MATCHES)
            val cachedAt = data?.get(KEY_CACHED_AT) ?: 0L

            val initialMatches = if (!cachedJson.isNullOrBlank()) {
                runCatching {
                    json.decodeFromString(ListSerializer(MatchItem.serializer()), cachedJson)
                }.getOrElse { loadOfflineSnapshot() }
            } else {
                loadOfflineSnapshot()
            }

            _state.value = SportsUiState(
                matches = initialMatches,
                favoriteTeamKey = favTeam,
                isRefreshing = false,
            )

            // Refresh online if cache is older than 20 minutes
            if (System.currentTimeMillis() - cachedAt > CACHE_TTL_MS) {
                refreshOnline()
            }
        }
    }

    fun selectFavoriteTeam(teamKey: String) {
        _state.value = _state.value.copy(favoriteTeamKey = teamKey)
        scope.launch(Dispatchers.IO) {
            prefs.edit { it[KEY_FAV_TEAM] = teamKey }
        }
    }

    fun cycleFavoriteTeam() {
        val current = _state.value.favoriteTeamKey
        val idx = FAVORITE_TEAMS_PRESETS.indexOfFirst { it.keyEn == current }
        val next = FAVORITE_TEAMS_PRESETS[(idx + 1) % FAVORITE_TEAMS_PRESETS.size]
        selectFavoriteTeam(next.keyEn)
    }

    fun refreshOnline() {
        scope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isRefreshing = true)
            val liveMatches = ArrayList<MatchItem>()

            for (league in LEAGUES) {
                runCatching {
                    val raw = fetchHttps("https://site.api.espn.com/apis/site/v2/sports/soccer/${league.code}/scoreboard")
                    val parsed = parseEspnScoreboard(raw, league.code, league.nameAr, league.nameEn, json)
                    liveMatches.addAll(parsed)
                }.onFailure { t ->
                    Logger.w(Logger.TAG_SYSTEM, t) { "Skipped league ${league.code} refresh" }
                }
            }

            if (liveMatches.isNotEmpty()) {
                val sorted = sortMatches(liveMatches)
                _state.value = _state.value.copy(matches = sorted, isRefreshing = false)
                runCatching {
                    val encoded = json.encodeToString(ListSerializer(MatchItem.serializer()), sorted)
                    prefs.edit {
                        it[KEY_CACHED_MATCHES] = encoded
                        it[KEY_CACHED_AT] = System.currentTimeMillis()
                    }
                }
                Logger.system { "Refreshed ${sorted.size} live/upcoming matches across ${LEAGUES.size} leagues" }
            } else {
                _state.value = _state.value.copy(isRefreshing = false)
            }
        }
    }

    private suspend fun loadOfflineSnapshot(): List<MatchItem> = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(SNAPSHOT_ASSET).bufferedReader().use { r ->
                val list = json.decodeFromString(ListSerializer(MatchItem.serializer()), r.readText())
                sortMatches(list)
            }
        }.getOrElse { emptyList() }
    }

    private fun fetchHttps(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 7_000
            requestMethod = "GET"
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private data class LeagueSpec(val code: String, val nameAr: String, val nameEn: String)

    companion object {
        private const val SNAPSHOT_ASSET = "sports/matches_snapshot.json"
        private const val CACHE_TTL_MS = 20 * 60_000L
        val KEY_FAV_TEAM = stringPreferencesKey("sports_fav_team")
        val KEY_CACHED_MATCHES = stringPreferencesKey("sports_cached_matches")
        val KEY_CACHED_AT = longPreferencesKey("sports_cached_at")

        private val LEAGUES = listOf(
            LeagueSpec("caf.champions", "دوري أبطال أفريقيا", "CAF Champions League"),
            LeagueSpec("ksa.1", "الدوري السعودي (روشن)", "Saudi Pro League"),
            LeagueSpec("uefa.champions", "دوري أبطال أوروبا", "UEFA Champions League"),
            LeagueSpec("eng.1", "الدوري الإنجليزي الممتاز", "Premier League"),
            LeagueSpec("esp.1", "الدوري الإسباني (لا ليغا)", "LaLiga"),
            LeagueSpec("ita.1", "الدوري الإيطالي", "Serie A"),
            LeagueSpec("ger.1", "الدوري الألماني", "Bundesliga"),
        )

        private val TEAM_AR_MAP = mapOf(
            "Al Ahly" to "الأهلي المصري",
            "Zamalek" to "الزمالك",
            "Pyramids FC" to "بيراميدز",
            "Espérance Sportive de Tunis" to "الترجي التونسي",
            "Wydad AC" to "الوداد المغربي",
            "Raja Casablanca" to "الرجاء المغربي",
            "Mamelodi Sundowns" to "صن داونز",
            "CR Belouizdad" to "شباب بلوزداد",
            "MC Alger" to "مولودية الجزائر",
            "Al Hilal" to "الهلال السعودي",
            "Al Nassr" to "النصر السعودي",
            "Al Ittihad" to "الاتحاد السعودي",
            "Al Ahli" to "الأهلي السعودي",
            "Al Shabab" to "الشباب السعودي",
            "Al Ettifaq" to "الاتفاق",
            "Liverpool" to "ليفربول",
            "Manchester City" to "مانشستر سيتي",
            "Arsenal" to "آرسنال",
            "Chelsea" to "تشيلسي",
            "Manchester United" to "مانشستر يونايتد",
            "Tottenham Hotspur" to "توتنهام",
            "Newcastle United" to "نيوكاسل يونايتد",
            "Aston Villa" to "أستون فيلا",
            "Brighton & Hove Albion" to "برايتون",
            "Brentford" to "برينتفورد",
            "Crystal Palace" to "كريستال بالاس",
            "AFC Bournemouth" to "بورنموث",
            "Ipswich Town" to "إبسويتش تاون",
            "Real Madrid" to "ريال مدريد",
            "Barcelona" to "برشلونة",
            "Atletico Madrid" to "أتلتيكو مدريد",
            "Atlético Madrid" to "أتلتيكو مدريد",
            "Athletic Club" to "أتلتيك بلباو",
            "Real Sociedad" to "ريال سوسيداد",
            "Villarreal" to "فياريال",
            "Real Betis" to "ريال بيتيس",
            "Girona" to "جيرونا",
            "Bayern Munich" to "بايرن ميونخ",
            "Borussia Dortmund" to "بوروسيا دورتموند",
            "Bayer Leverkusen" to "باير ليفركوزن",
            "Paris Saint-Germain" to "باريس سان جيرمان",
            "Inter Milan" to "إنتر ميلان",
            "Internazionale" to "إنتر ميلان",
            "AC Milan" to "ميلان",
            "Juventus" to "يوفنتوس",
            "Napoli" to "نابولي",
            "AS Roma" to "روما",
            "Atalanta" to "أتلانتا",
        )

        fun translateTeamAr(enName: String): String = TEAM_AR_MAP[enName] ?: enName

        internal fun parseEspnScoreboard(
            rawJson: String,
            leagueCode: String,
            leagueAr: String,
            leagueEn: String,
            json: Json = Json { ignoreUnknownKeys = true },
        ): List<MatchItem> {
            val dto = json.decodeFromString(EspnScoreboardDto.serializer(), rawJson)
            return dto.events.mapNotNull { ev ->
                val comp = ev.competitions.firstOrNull() ?: return@mapNotNull null
                if (comp.competitors.size < 2) return@mapNotNull null
                val home = comp.competitors.firstOrNull { it.homeAway == "home" } ?: comp.competitors[0]
                val away = comp.competitors.firstOrNull { it.homeAway == "away" } ?: comp.competitors[1]
                val homeEn = home.team.displayName.trim()
                val awayEn = away.team.displayName.trim()
                MatchItem(
                    id = ev.id,
                    leagueCode = leagueCode,
                    leagueAr = leagueAr,
                    leagueEn = leagueEn,
                    homeAr = translateTeamAr(homeEn),
                    homeEn = homeEn,
                    awayAr = translateTeamAr(awayEn),
                    awayEn = awayEn,
                    homeScore = home.score.toIntOrNull() ?: 0,
                    awayScore = away.score.toIntOrNull() ?: 0,
                    state = ev.status.type.state,
                    dateIso = ev.date,
                    statusText = ev.status.type.shortDetail,
                )
            }
        }

        internal fun sortMatches(list: List<MatchItem>): List<MatchItem> {
            // Order: Live ("in") first, then Upcoming ("pre"), then Finished ("post")
            return list.sortedWith(
                compareBy<MatchItem> {
                    when (it.state) {
                        "in" -> 0
                        "pre" -> 1
                        else -> 2
                    }
                }.thenBy { it.dateIso },
            )
        }
    }
}

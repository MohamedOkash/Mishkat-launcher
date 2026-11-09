package com.mishkat.launcher.radio

import android.content.Context
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class Mp3QuranRadiosResponse(
    val radios: List<Mp3QuranRadioDto> = emptyList(),
)

@Serializable
private data class Mp3QuranRadioDto(
    val id: Int,
    val name: String,
    val url: String,
)

/**
 * §3 + §9 + R3:
 * - First run seeds Room offline from `assets/stations_snapshot.json` (177 stations, zero network needed).
 * - Optional online refresh merges updated stream URLs/names from `https://mp3quran.net/api/v3/radios`
 *   while preserving user favorites (`isFavorite`) and play history (`lastPlayedAt`).
 */
class StationRepository(
    private val context: Context,
    private val dao: StationDao,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _stations = MutableStateFlow<List<StationEntity>>(emptyList())
    val stations: StateFlow<List<StationEntity>> = _stations.asStateFlow()

    fun start() {
        scope.launch(Dispatchers.IO) {
            val priorityIndex = HashMap<Int, Int>()
            runCatching {
                val snapshot = loadSnapshot()
                snapshot.forEachIndexed { idx, station -> priorityIndex[station.id] = idx }
                dao.insertIgnore(snapshot)
                // Ensure curated names & fixed URLs for all stations are synced on upgrade
                for (s in snapshot) {
                    dao.updateMetadata(s.id, s.nameAr, s.nameEn, s.url)
                }
                Logger.radio { "Synced ${snapshot.size} stations from offline snapshot" }
            }.onFailure { t ->
                Logger.e(Logger.TAG_RADIO, t) { "Failed to seed stations snapshot" }
            }

            dao.observeAll().collect { list ->
                _stations.value = list.sortedWith(
                    compareByDescending<StationEntity> { it.id == 100001 || it.id == 109082 }
                        .thenBy { priorityIndex[it.id] ?: (10_000 + it.id) }
                        .thenByDescending { it.isFavorite }
                        .thenByDescending { it.lastPlayedAt },
                )
            }
        }
    }

    fun toggleFavorite(stationId: Int) {
        scope.launch(Dispatchers.IO) {
            runCatching { dao.toggleFavorite(stationId) }
        }
    }

    fun markPlayed(stationId: Int) {
        scope.launch(Dispatchers.IO) {
            runCatching { dao.markPlayed(stationId, System.currentTimeMillis()) }
        }
    }

    /** Optional online update from mp3quran API v3 (HTTPS, R22). */
    fun refreshOnline() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val arText = fetchHttps("https://mp3quran.net/api/v3/radios?language=ar")
                val arRadios = json.decodeFromString(Mp3QuranRadiosResponse.serializer(), arText).radios
                if (arRadios.isEmpty()) return@runCatching

                val existingIds = dao.getAll().map { it.id }.toHashSet()
                val newEntities = ArrayList<StationEntity>()
                for (dto in arRadios) {
                    val cleanName = dto.name.trim()
                    val cleanUrl = dto.url.trim()
                    if (dto.id in existingIds) {
                        dao.updateMetadata(dto.id, cleanName, cleanName, cleanUrl)
                    } else {
                        newEntities += StationEntity(
                            id = dto.id,
                            nameAr = cleanName,
                            nameEn = cleanName,
                            url = cleanUrl,
                        )
                    }
                }
                if (newEntities.isNotEmpty()) dao.insertIgnore(newEntities)
                Logger.radio { "Refreshed ${arRadios.size} stations from mp3quran API" }
            }.onFailure { t ->
                Logger.w(Logger.TAG_RADIO, t) { "Online station refresh skipped (offline fallback active)" }
            }
        }
    }

    private suspend fun loadSnapshot(): List<StationEntity> = withContext(Dispatchers.IO) {
        context.assets.open(SNAPSHOT_ASSET).bufferedReader().use { reader ->
            val items = json.decodeFromString(
                ListSerializer(SnapshotStation.serializer()),
                reader.readText(),
            )
            items.map { s ->
                StationEntity(
                    id = s.id,
                    nameAr = s.nameAr,
                    nameEn = s.nameEn,
                    url = s.url,
                    // Pre-mark the first 4 iconic stations as initial favorites for instant 1-click access.
                    isFavorite = s.id in DEFAULT_FAVORITE_IDS,
                )
            }
        }
    }

    private fun fetchHttps(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val SNAPSHOT_ASSET = "stations_snapshot.json"
        val DEFAULT_FAVORITE_IDS = setOf(100001, 109082, 30, 70, 75, 80, 79, 63)
    }
}

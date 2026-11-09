package com.mishkat.launcher.radio

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class SnapshotStation(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val url: String,
)

@Immutable
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: Int,
    val nameAr: String,
    val nameEn: String,
    val url: String,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long = 0L,
) {
    fun displayName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn.ifBlank { nameAr }

    /** True if this station is a thematic channel (Tafsir, Fatwa, Ruqyah, Seerah, Adhkar, General) rather than a single reciter. */
    val isThematicChannel: Boolean
        get() = nameAr.contains("إذاعة") ||
            nameAr.contains("تفسير") ||
            nameAr.contains("الفتاوى") ||
            nameAr.contains("الرقية") ||
            nameAr.contains("أذكار") ||
            nameAr.contains("السيرة") ||
            nameAr.contains("تراتيل") ||
            nameAr.contains("آيات السكينة")
}

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY isFavorite DESC, lastPlayedAt DESC, id ASC")
    fun observeAll(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations ORDER BY isFavorite DESC, lastPlayedAt DESC, id ASC")
    suspend fun getAll(): List<StationEntity>

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(stations: List<StationEntity>)

    @Query("UPDATE stations SET nameAr = :nameAr, nameEn = :nameEn, url = :url WHERE id = :id")
    suspend fun updateMetadata(id: Int, nameAr: String, nameEn: String, url: String)

    @Query("UPDATE stations SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Int)

    @Query("UPDATE stations SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun markPlayed(id: Int, timestamp: Long)
}

@Database(entities = [StationEntity::class], version = 2, exportSchema = false)
abstract class MishkatDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        @Volatile
        private var instance: MishkatDatabase? = null

        fun get(context: Context): MishkatDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MishkatDatabase::class.java,
                    "mishkat.db",
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
            }
    }
}

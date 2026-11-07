package com.bopape.studentassistant.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * A read-only offline cache: the last JSON list the API returned for one user and
 * one collection (modules, timetable, exams, study sessions). When the phone is
 * offline these screens still show the most recent data instead of an error.
 */
@Entity(tableName = "api_cache", primaryKeys = ["userId", "collection"])
data class CacheEntity(
    val userId: String,
    val collection: String,
    val json: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM api_cache WHERE userId = :userId AND collection = :collection")
    suspend fun get(userId: String, collection: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntity)

    @Query("DELETE FROM api_cache WHERE userId = :userId")
    suspend fun clearUser(userId: String)
}

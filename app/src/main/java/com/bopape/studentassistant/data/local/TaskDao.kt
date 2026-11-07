package com.bopape.studentassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the offline task table. All queries are scoped to one user so
 * two students sharing a phone never see each other's cached tasks.
 * Reference: Android Developers (2026) Accessing data using Room DAOs.
 * https://developer.android.com/training/data-storage/room/accessing-data
 */
@Dao
interface TaskDao {

    /** Tasks to show in the UI (rows waiting to be deleted on the server are hidden). */
    @Query("SELECT * FROM tasks WHERE userId = :userId AND syncState != 'PENDING_DELETE' ORDER BY dueDate")
    suspend fun getVisible(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId")
    suspend fun getAll(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND syncState != 'SYNCED' ORDER BY updatedAt")
    suspend fun getPending(userId: String): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND syncState != 'SYNCED'")
    fun observePendingCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND syncState != 'SYNCED'")
    suspend fun pendingCount(userId: String): Int

    @Query("SELECT * FROM tasks WHERE localId = :localId AND userId = :userId")
    suspend fun getByLocalId(localId: Int, userId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Int)

    @Query("DELETE FROM tasks WHERE localId IN (:localIds)")
    suspend fun deleteByLocalIds(localIds: List<Int>)

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun clearUser(userId: String)
}

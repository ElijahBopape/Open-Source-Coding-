package com.bopape.studentassistant.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bopape.studentassistant.data.model.Task

/**
 * Where a locally stored task stands relative to the REST API.
 * SYNCED rows match the server; the PENDING_* states are changes made on this
 * device (possibly offline) that the sync engine still has to upload.
 */
object SyncState {
    const val SYNCED = "SYNCED"
    const val PENDING_CREATE = "PENDING_CREATE"
    const val PENDING_UPDATE = "PENDING_UPDATE"
    const val PENDING_DELETE = "PENDING_DELETE"
}

/**
 * Room table for tasks - the offline-first feature of the app (Final PoE).
 * Every task the user sees comes from this table, so the Tasks screen and the
 * dashboard work with no connection at all. [localId] never changes; [remoteId]
 * is filled in once the API has accepted the task.
 *
 * Reference: Android Developers (2026) Defining data using Room entities.
 * https://developer.android.com/training/data-storage/room/defining-data
 */
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["userId"]), Index(value = ["userId", "remoteId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val remoteId: Int? = null,
    val userId: String,
    val title: String,
    val description: String = "",
    val moduleId: Int? = null,
    val dueDate: String,
    val priority: String,
    val completed: Boolean = false,
    val syncState: String = SyncState.PENDING_CREATE,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** The UI model: id is the local id, so screens never need to know about sync. */
    fun toTask() = Task(
        id = localId,
        title = title,
        description = description,
        moduleId = moduleId,
        dueDate = dueDate,
        priority = priority,
        completed = completed,
        remoteId = remoteId,
        pendingSync = syncState != SyncState.SYNCED
    )

    /** The body sent to the REST API (the server assigns/uses its own id). */
    fun toApiTask() = Task(
        id = remoteId ?: 0,
        title = title,
        description = description,
        moduleId = moduleId,
        dueDate = dueDate,
        priority = priority,
        completed = completed
    )

    companion object {
        /** Builds a SYNCED row from a task the server returned. */
        fun fromRemote(task: Task, userId: String, localId: Int = 0) = TaskEntity(
            localId = localId,
            remoteId = task.id,
            userId = userId,
            title = task.title,
            description = task.description,
            moduleId = task.moduleId,
            dueDate = task.dueDate,
            priority = task.priority,
            completed = task.completed,
            syncState = SyncState.SYNCED
        )
    }
}

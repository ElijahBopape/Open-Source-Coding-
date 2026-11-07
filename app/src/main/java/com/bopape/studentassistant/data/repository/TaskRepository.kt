package com.bopape.studentassistant.data.repository

import android.util.Log
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.local.SyncState
import com.bopape.studentassistant.data.local.TaskDao
import com.bopape.studentassistant.data.local.TaskEntity
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.sync.SyncScheduler
import com.bopape.studentassistant.data.sync.TaskSyncEngine
import com.bopape.studentassistant.util.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Offline-first task storage (Final PoE "Offline mode with Sync").
 *
 * Every read and write goes to the local Room table first, so adding, editing,
 * completing and deleting tasks all work with no connection. Each write marks
 * the row as pending and asks WorkManager to sync; the upload happens straight
 * away when online, or automatically once the phone reconnects.
 */
class TaskRepository(private val dao: TaskDao = AppGraph.database.taskDao()) {

    private val tag = "TaskRepository"
    private val userId: String get() = AppGraph.currentUserId.orEmpty()

    /** Tasks from the local database - instant, and works offline. */
    suspend fun getTasks(): ApiResult<List<Task>> =
        ApiResult.Success(dao.getVisible(userId).map { it.toTask() })

    /** Pushes pending changes and pulls the latest server copy right now. */
    suspend fun syncNow(): TaskSyncEngine.Outcome = TaskSyncEngine(dao).sync(userId)

    fun observePendingCount(): Flow<Int> =
        if (userId.isEmpty()) flowOf(0) else dao.observePendingCount(userId)

    suspend fun pendingCount(): Int = dao.pendingCount(userId)

    suspend fun createTask(task: Task): ApiResult<Task> {
        val entity = TaskEntity(
            userId = userId,
            title = task.title,
            description = task.description,
            moduleId = task.moduleId,
            dueDate = task.dueDate,
            priority = task.priority,
            completed = task.completed,
            syncState = SyncState.PENDING_CREATE
        )
        val localId = dao.insert(entity).toInt()
        Log.d(tag, "Saved task '${task.title}' locally as #$localId (pending upload)")
        SyncScheduler.requestSync()
        return ApiResult.Success(entity.copy(localId = localId).toTask())
    }

    suspend fun updateTask(task: Task): ApiResult<Task> {
        val existing = dao.getByLocalId(task.id, userId)
            ?: return ApiResult.Failure(AppGraph.localizedContext().getString(R.string.error_not_found))

        // A task that was never uploaded stays a CREATE; otherwise it becomes an UPDATE.
        val newState = if (existing.syncState == SyncState.PENDING_CREATE) SyncState.PENDING_CREATE else SyncState.PENDING_UPDATE
        val updated = existing.copy(
            title = task.title,
            description = task.description,
            moduleId = task.moduleId,
            dueDate = task.dueDate,
            priority = task.priority,
            completed = task.completed,
            syncState = newState,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)
        Log.d(tag, "Updated task #${task.id} locally ($newState)")
        SyncScheduler.requestSync()
        return ApiResult.Success(updated.toTask())
    }

    suspend fun deleteTask(localId: Int): ApiResult<Unit> {
        val existing = dao.getByLocalId(localId, userId) ?: return ApiResult.Success(Unit)
        if (existing.remoteId == null) {
            // Never reached the server, so there is nothing to delete remotely.
            dao.deleteByLocalId(localId)
        } else {
            dao.update(existing.copy(syncState = SyncState.PENDING_DELETE, updatedAt = System.currentTimeMillis()))
            SyncScheduler.requestSync()
        }
        Log.d(tag, "Deleted task #$localId locally")
        return ApiResult.Success(Unit)
    }
}

package com.bopape.studentassistant.data.sync

import android.util.Log
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.data.local.SyncState
import com.bopape.studentassistant.data.local.TaskDao
import com.bopape.studentassistant.data.local.TaskEntity
import com.bopape.studentassistant.data.remote.ApiService
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.SessionManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * Two-way synchronisation of the offline task table with the REST API.
 *
 *  1. PUSH - every PENDING_* row is uploaded in the order it was changed:
 *       PENDING_CREATE -> POST   (the server id is saved as remoteId)
 *       PENDING_UPDATE -> PUT    (re-created with POST if the server lost it)
 *       PENDING_DELETE -> DELETE (then the local row is removed)
 *  2. PULL - the server list is fetched and merged in with [TaskMerge].
 *
 * If the network drops part-way, the remaining rows simply stay pending and
 * WorkManager retries later, so no offline change is ever lost.
 * A Mutex stops the background worker and a manual "Sync now" running at once.
 */
class TaskSyncEngine(
    private val dao: TaskDao = AppGraph.database.taskDao(),
    private val api: ApiService = RetrofitClient.apiService
) {

    data class Outcome(val success: Boolean, val pushed: Int, val pulled: Int, val networkError: Boolean = false)

    suspend fun sync(userId: String): Outcome = mutex.withLock {
        Log.i(TAG, "Sync started for $userId")
        var pushed = 0
        try {
            for (entity in dao.getPending(userId)) {
                if (pushOne(entity)) pushed++ else return@withLock Outcome(false, pushed, 0)
            }

            val response = api.getTasks()
            if (!response.isSuccessful) {
                Log.w(TAG, "Pull failed: HTTP ${response.code()}")
                return@withLock Outcome(false, pushed, 0)
            }
            val remote = response.body().orEmpty()
            val merge = TaskMerge.merge(dao.getAll(userId), remote, userId)
            if (merge.upserts.isNotEmpty()) dao.upsertAll(merge.upserts)
            if (merge.deleteLocalIds.isNotEmpty()) dao.deleteByLocalIds(merge.deleteLocalIds)

            SessionManager(AppGraph.context).lastSyncMillis = System.currentTimeMillis()
            Log.i(TAG, "Sync finished: pushed=$pushed, server=${remote.size}, " +
                "updated=${merge.upserts.size}, removed=${merge.deleteLocalIds.size}")
            SyncBus.notifyChanged()
            Outcome(true, pushed, merge.upserts.size)
        } catch (e: IOException) {
            Log.w(TAG, "Sync stopped - device is offline (${pushed} change(s) uploaded so far)")
            if (pushed > 0) SyncBus.notifyChanged()
            Outcome(false, pushed, 0, networkError = true)
        }
    }

    /** Uploads one pending change. Returns false if the server rejected it. */
    private suspend fun pushOne(entity: TaskEntity): Boolean {
        when (entity.syncState) {
            SyncState.PENDING_CREATE -> return create(entity)

            SyncState.PENDING_UPDATE -> {
                val remoteId = entity.remoteId ?: return create(entity)
                val response = api.updateTask(remoteId, entity.toApiTask())
                return when {
                    response.isSuccessful -> {
                        dao.update(entity.copy(syncState = SyncState.SYNCED))
                        Log.d(TAG, "Uploaded edit to task #$remoteId")
                        true
                    }
                    // Deleted on the server while we were offline: keep the user's edit by re-creating it.
                    response.code() == 404 -> create(entity)
                    else -> logRejected(entity, response.code())
                }
            }

            SyncState.PENDING_DELETE -> {
                val remoteId = entity.remoteId
                if (remoteId != null) {
                    val response = api.deleteTask(remoteId)
                    if (!response.isSuccessful && response.code() != 404) return logRejected(entity, response.code())
                }
                dao.deleteByLocalId(entity.localId)
                Log.d(TAG, "Deleted task #$remoteId on the server")
                return true
            }
        }
        return true
    }

    private suspend fun create(entity: TaskEntity): Boolean {
        val response = api.createTask(entity.toApiTask())
        val created = response.body()
        if (!response.isSuccessful || created == null) return logRejected(entity, response.code())
        dao.update(entity.copy(remoteId = created.id, syncState = SyncState.SYNCED))
        Log.d(TAG, "Uploaded offline task '${entity.title}' as #${created.id}")
        return true
    }

    private fun logRejected(entity: TaskEntity, code: Int): Boolean {
        Log.e(TAG, "Server rejected ${entity.syncState} for local task #${entity.localId}: HTTP $code")
        return false
    }

    private companion object {
        const val TAG = "TaskSyncEngine"
        val mutex = Mutex()
    }
}

package com.bopape.studentassistant.data.sync

import com.bopape.studentassistant.data.local.SyncState
import com.bopape.studentassistant.data.local.TaskEntity
import com.bopape.studentassistant.data.model.Task

/**
 * Decides how the server's copy of the task list is merged into the local Room
 * table after pending changes have been uploaded. Kept free of Android and
 * network code so the rules are covered by JVM unit tests (TaskMergeTest).
 *
 * Conflict policy ("local pending change wins"):
 *  - a task on the server that is unknown locally is added as SYNCED;
 *  - a SYNCED local task takes the server's latest values;
 *  - a local task with a pending change is left alone - the user's newer,
 *    not-yet-uploaded edit is kept and will be pushed on the next sync;
 *  - a SYNCED local task that no longer exists on the server (deleted on
 *    another device) is removed locally.
 */
object TaskMerge {

    data class Result(val upserts: List<TaskEntity>, val deleteLocalIds: List<Int>)

    fun merge(local: List<TaskEntity>, remote: List<Task>, userId: String): Result {
        val localByRemoteId = local.filter { it.remoteId != null }.associateBy { it.remoteId }
        val remoteIds = remote.map { it.id }.toSet()

        val upserts = mutableListOf<TaskEntity>()
        for (serverTask in remote) {
            val existing = localByRemoteId[serverTask.id]
            when {
                existing == null -> upserts += TaskEntity.fromRemote(serverTask, userId)
                existing.syncState == SyncState.SYNCED -> {
                    val fresh = TaskEntity.fromRemote(serverTask, userId, existing.localId)
                    // Only write rows that actually changed, to avoid needless DB churn.
                    if (fresh.copy(updatedAt = existing.updatedAt) != existing) upserts += fresh
                }
                else -> Unit // pending local change wins
            }
        }

        val deletes = local
            .filter { it.syncState == SyncState.SYNCED && it.remoteId != null && it.remoteId !in remoteIds }
            .map { it.localId }

        return Result(upserts, deletes)
    }
}

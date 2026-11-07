package com.bopape.studentassistant

import com.bopape.studentassistant.data.local.SyncState
import com.bopape.studentassistant.data.local.TaskEntity
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.sync.TaskMerge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the offline-sync merge rules (TaskMerge): how the server's task list is
 * combined with the local Room table after pending changes have been uploaded.
 */
class TaskMergeTest {

    private val user = "student-1"

    private fun local(localId: Int, remoteId: Int?, title: String, state: String = SyncState.SYNCED) =
        TaskEntity(localId = localId, remoteId = remoteId, userId = user, title = title,
            dueDate = "2026-11-20", priority = "Medium", syncState = state, updatedAt = 1L)

    private fun remote(id: Int, title: String) = Task(id = id, title = title, dueDate = "2026-11-20", priority = "Medium")

    @Test
    fun newServerTask_isInsertedAsSynced() {
        val result = TaskMerge.merge(local = emptyList(), remote = listOf(remote(7, "From web")), userId = user)

        assertEquals(1, result.upserts.size)
        assertEquals(7, result.upserts[0].remoteId)
        assertEquals(SyncState.SYNCED, result.upserts[0].syncState)
    }

    @Test
    fun syncedLocalTask_takesServerValues_andKeepsLocalId() {
        val result = TaskMerge.merge(listOf(local(3, 7, "Old title")), listOf(remote(7, "New title")), user)

        assertEquals(1, result.upserts.size)
        assertEquals(3, result.upserts[0].localId)
        assertEquals("New title", result.upserts[0].title)
    }

    @Test
    fun unchangedTask_isNotRewritten() {
        val result = TaskMerge.merge(listOf(local(3, 7, "Same")), listOf(remote(7, "Same")), user)
        assertTrue(result.upserts.isEmpty())
        assertTrue(result.deleteLocalIds.isEmpty())
    }

    @Test
    fun pendingLocalEdit_winsOverServerCopy() {
        val pending = local(3, 7, "Edited offline", SyncState.PENDING_UPDATE)
        val result = TaskMerge.merge(listOf(pending), listOf(remote(7, "Server title")), user)

        assertTrue("A pending local edit must not be overwritten", result.upserts.isEmpty())
        assertTrue(result.deleteLocalIds.isEmpty())
    }

    @Test
    fun syncedTaskDeletedOnServer_isRemovedLocally() {
        val result = TaskMerge.merge(listOf(local(3, 7, "Gone")), remote = emptyList(), userId = user)
        assertEquals(listOf(3), result.deleteLocalIds)
    }

    @Test
    fun offlineCreatedTask_isNeverDeletedByMerge() {
        val created = local(9, null, "Made offline", SyncState.PENDING_CREATE)
        val result = TaskMerge.merge(listOf(created), remote = emptyList(), userId = user)
        assertTrue(result.deleteLocalIds.isEmpty())
    }
}

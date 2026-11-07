package com.bopape.studentassistant.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.data.repository.ExamRepository
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.StudySessionRepository
import com.bopape.studentassistant.data.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.TimeUnit

/**
 * Background sync job run by WorkManager. Because it is enqueued with a
 * "network connected" constraint, an offline change made on the Tasks screen is
 * uploaded automatically as soon as the phone reconnects - even if the app has
 * been closed in the meantime.
 *
 * Reference: Android Developers (2026) Schedule tasks with WorkManager.
 * https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = AppGraph.currentUserId ?: return Result.success()
        val outcome = TaskSyncEngine().sync(userId)

        // Refresh the read-only offline caches so other screens work offline too.
        ModuleRepository().getModules()
        TimetableRepository().getTimetable()
        ExamRepository().getExams()
        StudySessionRepository().getSessions()

        Log.i(TAG, "SyncWorker finished: $outcome (attempt ${runAttemptCount + 1})")
        return if (outcome.success) Result.success() else Result.retry()
    }

    private companion object {
        const val TAG = "SyncWorker"
    }
}

/** Starts sync work. requestSync() is called after every offline write. */
object SyncScheduler {

    private const val ONE_TIME = "task-sync"
    private const val PERIODIC = "task-sync-periodic"

    private val connected = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun requestSync(context: Context = AppGraph.context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: a change made while a sync is running gets its own run afterwards.
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        Log.d("SyncScheduler", "Sync requested (runs when the device is online)")
    }

    /** A safety-net sync every 6 hours while the app is installed. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(connected)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME)
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }
}

/** Lets open screens reload as soon as a sync has changed the local data. */
object SyncBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifyChanged() {
        _events.tryEmit(Unit)
    }
}

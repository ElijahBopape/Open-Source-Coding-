package com.bopape.studentassistant.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.repository.ExamRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.SessionManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Local daily deadline reminders, complementing the server's FCM pushes.
 * Runs once a day (first run around 07:00) and needs no network: it reads the
 * offline task table and the cached exam list, so a student is still reminded
 * of tomorrow's deadline while offline.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (AppGraph.currentUserId == null || !SessionManager(applicationContext).notificationsEnabled) {
            return Result.success()
        }
        val text = AppGraph.localizedContext()

        val tasks = (TaskRepository().getTasks() as? ApiResult.Success)?.data.orEmpty()
        tasks.filter { !it.completed }.forEach { task ->
            when (DateUtils.daysUntil(task.dueDate)) {
                0L -> NotificationHelper.show(applicationContext, NotificationHelper.CHANNEL_REMINDERS,
                    text.getString(R.string.reminder_due_today), task.title, "task")
                1L -> NotificationHelper.show(applicationContext, NotificationHelper.CHANNEL_REMINDERS,
                    text.getString(R.string.reminder_due_tomorrow), task.title, "task")
            }
        }

        val exams = (ExamRepository().getExams() as? ApiResult.Success)?.data.orEmpty()
        exams.filter { (DateUtils.daysUntil(it.date) ?: -1) in 0..3 }.forEach { exam ->
            NotificationHelper.show(applicationContext, NotificationHelper.CHANNEL_REMINDERS,
                text.getString(R.string.reminder_exam_soon), "${exam.title} - ${exam.date} ${exam.time}".trim(), "exam")
        }
        Log.i(TAG, "Daily reminders checked")
        return Result.success()
    }

    companion object {
        private const val TAG = "ReminderWorker"
        private const val WORK_NAME = "daily-reminders"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(7, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

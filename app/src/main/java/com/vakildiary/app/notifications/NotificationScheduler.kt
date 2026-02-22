package com.vakildiary.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val DAILY_DIGEST_WORK_NAME = "daily_digest_work"

    fun scheduleDailyDigest(context: Context) {
        NotificationChannels.ensureChannels(context)

        val initialDelayMillis = computeInitialDelayTo8AmMillis()
        val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_DIGEST_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleHearingReminder(context: Context, caseId: String, triggerAtMillis: Long) {
        NotificationChannels.ensureChannels(context)

        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(HearingReminderWorker.KEY_CASE_ID, caseId)
            .build()

        val request = OneTimeWorkRequestBuilder<HearingReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "hearing_reminder_$caseId_$triggerAtMillis",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleTaskReminder(context: Context, taskId: String, triggerAtMillis: Long) {
        NotificationChannels.ensureChannels(context)

        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(TaskReminderWorker.KEY_TASK_ID, taskId)
            .build()

        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "task_reminder_$taskId_$triggerAtMillis",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun computeInitialDelayTo8AmMillis(): Long {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        var next = now.toLocalDate().atTime(LocalTime.of(8, 0))
        if (now >= next) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMillis()
    }
}

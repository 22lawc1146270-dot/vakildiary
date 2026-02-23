package com.vakildiary.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vakildiary.app.domain.model.BackupSchedule
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val BACKUP_WORK_NAME = "drive_backup_work"

    fun scheduleBackup(context: Context, schedule: BackupSchedule) {
        val workManager = WorkManager.getInstance(context)
        if (schedule == BackupSchedule.MANUAL) {
            workManager.cancelUniqueWork(BACKUP_WORK_NAME)
            return
        }

        val request = when (schedule) {
            BackupSchedule.DAILY -> PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            BackupSchedule.WEEKLY -> PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
            BackupSchedule.MANUAL -> null
        }

        request?.setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        )?.build()?.let { work ->
            workManager.enqueueUniquePeriodicWork(
                BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                work
            )
        }
    }
}

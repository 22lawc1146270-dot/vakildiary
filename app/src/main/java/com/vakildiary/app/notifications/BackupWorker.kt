package com.vakildiary.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vakildiary.app.core.Result as AppResult
import com.vakildiary.app.domain.model.BackupSchedule
import kotlinx.coroutines.flow.first

class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = WorkerEntryPointAccessors.userPreferencesRepository(applicationContext)
        val schedule = prefs.backupSchedule.first()
        if (schedule == BackupSchedule.MANUAL) return Result.success()

        val manager = WorkerEntryPointAccessors.manualBackupManager(applicationContext)
        return when (manager.backupNow()) {
            is AppResult.Success -> Result.success()
            is AppResult.Error -> Result.failure()
        }
    }
}

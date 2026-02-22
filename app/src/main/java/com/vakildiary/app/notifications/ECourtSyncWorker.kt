package com.vakildiary.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vakildiary.app.R
import com.vakildiary.app.data.local.dao.CaseDao

class ECourtSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        NotificationChannels.ensureChannels(applicationContext)

        val caseDao: CaseDao = WorkerEntryPointAccessors.caseDao(applicationContext)
        val trackedCases = caseDao.getECourtTrackedCases()

        if (trackedCases.isEmpty()) return Result.success()

        // Placeholder: real eCourt polling should parse eCourt response for next hearing date.
        // Here we just notify if any tracked cases exist.
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.ECOURT_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("eCourt Sync")
            .setContentText("Checked ${trackedCases.size} tracked cases")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 3001
    }
}

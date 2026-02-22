package com.vakildiary.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vakildiary.app.R
import com.vakildiary.app.data.local.dao.CaseDao
import kotlinx.coroutines.flow.first

class HearingReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        NotificationChannels.ensureChannels(applicationContext)

        val caseId = inputData.getString(KEY_CASE_ID) ?: return Result.failure()
        val caseDao: CaseDao = WorkerEntryPointAccessors.caseDao(applicationContext)
        val caseEntity = caseDao.getCaseById(caseId).first() ?: return Result.failure()

        val title = "Hearing Reminder"
        val content = "${caseEntity.caseName} • ${caseEntity.courtName}"

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.HEARING_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(caseId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_CASE_ID = "case_id"
    }
}

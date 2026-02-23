package com.vakildiary.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.dao.TaskDao
import com.vakildiary.app.data.backup.ManualBackupManager
import com.vakildiary.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class DailyDigestWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val entryPoint = EntryPointAccessors.fromApplication(
        appContext,
        WorkerEntryPoint::class.java
    )
    private val caseDao: CaseDao = entryPoint.caseDao()
    private val taskDao: TaskDao = entryPoint.taskDao()

    override suspend fun doWork(): Result {
        NotificationChannels.ensureChannels(applicationContext)

        val hearings = caseDao.getCasesWithHearingToday().first()
        val tasks = taskDao.getTasksDueToday().first()

        val title = "Today's Digest"
        val content = "Hearings: ${hearings.size} • Tasks: ${tasks.size}"
        val style = NotificationCompat.InboxStyle()
        hearings.take(3).forEach { item ->
            style.addLine("Hearing: ${item.caseName}")
        }
        tasks.take(3).forEach { item ->
            style.addLine("Task: ${item.title}")
        }

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.DAILY_DIGEST)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(style)
            .setContentIntent(NotificationIntents.dashboard(applicationContext))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun caseDao(): CaseDao
    fun taskDao(): TaskDao
    fun ecourtRepository(): com.vakildiary.app.domain.repository.ECourtRepository
    fun ecourtTrackingStore(): com.vakildiary.app.data.ecourt.ECourtTrackingStore
    fun manualBackupManager(): ManualBackupManager
    fun userPreferencesRepository(): UserPreferencesRepository
}

package com.vakildiary.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vakildiary.app.R
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.dao.TaskDao
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

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.DAILY_DIGEST)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
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
}

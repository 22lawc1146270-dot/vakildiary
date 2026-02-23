package com.vakildiary.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vakildiary.app.data.local.dao.TaskDao
import com.vakildiary.app.data.local.dao.CaseDao
import kotlinx.coroutines.flow.first

class TaskReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        NotificationChannels.ensureChannels(applicationContext)

        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val taskDao: TaskDao = WorkerEntryPointAccessors.taskDao(applicationContext)
        val taskEntity = taskDao.getTaskById(taskId).first() ?: return Result.failure()
        val caseDao: CaseDao = WorkerEntryPointAccessors.caseDao(applicationContext)
        val caseEntity = caseDao.getCaseById(taskEntity.caseId).first()

        val title = "Task Reminder"
        val content = if (caseEntity == null) {
            taskEntity.title
        } else {
            "${taskEntity.title} • ${caseEntity.caseName}"
        }

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.TASK_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(NotificationIntents.caseDetail(applicationContext, taskEntity.caseId))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(taskId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}

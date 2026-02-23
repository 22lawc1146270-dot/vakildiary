package com.vakildiary.app.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.vakildiary.app.MainActivity

object NotificationIntents {
    const val EXTRA_CASE_ID = "extra_case_id"
    const val EXTRA_DESTINATION = "extra_destination"
    const val DEST_DASHBOARD = "dashboard"
    const val DEST_OVERDUE = "overdue_tasks"

    fun caseDetail(context: Context, caseId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CASE_ID, caseId)
        }
        return PendingIntent.getActivity(
            context,
            caseId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun dashboard(context: Context): PendingIntent {
        return destination(context, DEST_DASHBOARD, 1001)
    }

    fun overdueTasks(context: Context): PendingIntent {
        return destination(context, DEST_OVERDUE, 1002)
    }

    private fun destination(context: Context, destination: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, destination)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

package com.vakildiary.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val DAILY_DIGEST = "daily_digest"
    const val HEARING_REMINDER = "hearing_reminder"
    const val TASK_REMINDER = "task_reminder"
    const val ECOURT_ALERTS = "ecourt_alerts"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                DAILY_DIGEST,
                "Daily Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                HEARING_REMINDER,
                "Hearing Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                TASK_REMINDER,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                ECOURT_ALERTS,
                "eCourt Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }
}

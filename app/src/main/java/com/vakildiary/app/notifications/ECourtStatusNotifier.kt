package com.vakildiary.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ECourtStatusNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notifyStatusChange(caseId: String, caseName: String, message: String) {
        NotificationChannels.ensureChannels(context)
        val notification = NotificationCompat.Builder(context, NotificationChannels.ECOURT_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("eCourt Update")
            .setContentText("$caseName: $message")
            .setContentIntent(NotificationIntents.caseDetail(context, caseId))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(caseName.hashCode(), notification)
    }
}

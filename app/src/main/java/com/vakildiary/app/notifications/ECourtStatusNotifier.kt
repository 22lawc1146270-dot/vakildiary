package com.vakildiary.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vakildiary.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ECourtStatusNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notifyStatusChange(caseName: String, message: String) {
        NotificationChannels.ensureChannels(context)
        val notification = NotificationCompat.Builder(context, NotificationChannels.ECOURT_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("eCourt Update")
            .setContentText("$caseName: $message")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(caseName.hashCode(), notification)
    }
}

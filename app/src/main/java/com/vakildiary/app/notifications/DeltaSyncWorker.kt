package com.vakildiary.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vakildiary.app.data.backup.DeltaSyncManager
import dagger.hilt.android.EntryPointAccessors

class DeltaSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(appContext, DeltaSyncEntryPoint::class.java)
        val manager = entryPoint.deltaSyncManager()
        return when (manager.syncDocuments()) {
            is com.vakildiary.app.core.Result.Success -> Result.success()
            is com.vakildiary.app.core.Result.Error -> Result.retry()
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DeltaSyncEntryPoint {
    fun deltaSyncManager(): DeltaSyncManager
}

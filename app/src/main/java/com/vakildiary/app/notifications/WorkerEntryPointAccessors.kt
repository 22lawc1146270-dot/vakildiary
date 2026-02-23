package com.vakildiary.app.notifications

import android.content.Context
import com.vakildiary.app.data.ecourt.ECourtTrackingStore
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.dao.TaskDao
import com.vakildiary.app.data.backup.ManualBackupManager
import com.vakildiary.app.data.preferences.UserPreferencesRepository
import com.vakildiary.app.domain.repository.ECourtRepository
import dagger.hilt.android.EntryPointAccessors

object WorkerEntryPointAccessors {
    fun caseDao(context: Context): CaseDao {
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        return entryPoint.caseDao()
    }

    fun taskDao(context: Context): TaskDao {
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        return entryPoint.taskDao()
    }

    fun ecourtRepository(context: Context): ECourtRepository {
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        return entryPoint.ecourtRepository()
    }

    fun ecourtTrackingStore(context: Context): ECourtTrackingStore {
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        return entryPoint.ecourtTrackingStore()
    }

    fun manualBackupManager(context: Context): ManualBackupManager {
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        return entryPoint.manualBackupManager()
    }

    fun userPreferencesRepository(context: Context): UserPreferencesRepository {
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        return entryPoint.userPreferencesRepository()
    }
}

package com.vakildiary.app.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.data.preferences.UserPreferencesRepository
import com.vakildiary.app.domain.model.BackupLogEntry
import com.vakildiary.app.domain.model.BackupSchedule
import com.vakildiary.app.domain.model.BackupStatus
import com.vakildiary.app.notifications.BackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupStatusViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val backupSchedule: StateFlow<BackupSchedule> = userPreferencesRepository.backupSchedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupSchedule.MANUAL)

    val lastBackupTime: StateFlow<Long?> = userPreferencesRepository.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastBackupSizeBytes: StateFlow<Long?> = userPreferencesRepository.lastBackupSizeBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastBackupStatus: StateFlow<BackupStatus> = userPreferencesRepository.lastBackupStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupStatus.NONE)

    val lastBackupMessage: StateFlow<String?> = userPreferencesRepository.lastBackupMessage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val backupLog: StateFlow<List<BackupLogEntry>> = userPreferencesRepository.backupLog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setBackupSchedule(schedule: BackupSchedule) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupSchedule(schedule)
            BackupScheduler.scheduleBackup(context, schedule)
        }
    }
}

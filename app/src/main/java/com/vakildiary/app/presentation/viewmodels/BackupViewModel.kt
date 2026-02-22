package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.backup.ManualBackupManager
import com.vakildiary.app.presentation.viewmodels.state.BackupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val manualBackupManager: ManualBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun backupNow() {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            _uiState.value = when (val result = manualBackupManager.backupNow()) {
                is Result.Success -> BackupUiState.Success(result.data)
                is Result.Error -> BackupUiState.Error(result.message)
            }
        }
    }
}

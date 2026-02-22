package com.vakildiary.app.presentation.viewmodels.state

sealed interface BackupUiState {
    object Idle : BackupUiState
    object Loading : BackupUiState
    data class Success(val fileId: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

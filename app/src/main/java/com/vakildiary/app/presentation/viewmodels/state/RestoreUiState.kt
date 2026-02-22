package com.vakildiary.app.presentation.viewmodels.state

sealed interface RestoreUiState {
    object Idle : RestoreUiState
    object Checking : RestoreUiState
    object Available : RestoreUiState
    object NotAvailable : RestoreUiState
    object Restoring : RestoreUiState
    object Restored : RestoreUiState
    data class Error(val message: String) : RestoreUiState
}

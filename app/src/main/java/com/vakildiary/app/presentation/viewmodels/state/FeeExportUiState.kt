package com.vakildiary.app.presentation.viewmodels.state

import java.io.File

sealed interface FeeExportUiState {
    object Loading : FeeExportUiState
    data class Success(val file: File?) : FeeExportUiState
    data class Error(val message: String) : FeeExportUiState
}

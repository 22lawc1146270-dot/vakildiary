package com.vakildiary.app.presentation.viewmodels.state

import java.io.File

sealed interface CaseExportUiState {
    object Loading : CaseExportUiState
    data class Success(val file: File?) : CaseExportUiState
    data class Error(val message: String) : CaseExportUiState
}

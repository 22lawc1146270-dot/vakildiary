package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Case

sealed interface CasePickerUiState {
    object Loading : CasePickerUiState
    data class Success(val cases: List<Case>) : CasePickerUiState
    data class Error(val message: String) : CasePickerUiState
}

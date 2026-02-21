package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Case

sealed interface CaseListUiState {
    object Loading : CaseListUiState
    data class Success(val cases: List<Case>) : CaseListUiState
    data class Error(val message: String) : CaseListUiState
}

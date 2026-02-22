package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Case

sealed interface AddHearingUiState {
    object Loading : AddHearingUiState
    data class Success(val cases: List<Case>) : AddHearingUiState
    data class Error(val message: String) : AddHearingUiState
}

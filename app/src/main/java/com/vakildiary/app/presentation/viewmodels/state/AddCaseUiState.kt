package com.vakildiary.app.presentation.viewmodels.state

sealed interface AddCaseUiState {
    object Loading : AddCaseUiState
    data class Success(val isSaved: Boolean) : AddCaseUiState
    data class Error(val message: String) : AddCaseUiState
}

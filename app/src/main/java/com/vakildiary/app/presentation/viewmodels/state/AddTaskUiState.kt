package com.vakildiary.app.presentation.viewmodels.state

sealed interface AddTaskUiState {
    object Loading : AddTaskUiState
    data class Success(val isSaved: Boolean) : AddTaskUiState
    data class Error(val message: String) : AddTaskUiState
}

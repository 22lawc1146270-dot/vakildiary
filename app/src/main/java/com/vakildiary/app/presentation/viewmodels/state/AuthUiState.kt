package com.vakildiary.app.presentation.viewmodels.state

sealed interface AuthUiState {
    object Loading : AuthUiState
    data class Success(val email: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

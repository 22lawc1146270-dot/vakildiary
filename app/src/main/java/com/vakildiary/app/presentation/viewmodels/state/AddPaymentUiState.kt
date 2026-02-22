package com.vakildiary.app.presentation.viewmodels.state

sealed interface AddPaymentUiState {
    object Loading : AddPaymentUiState
    data class Success(val isSaved: Boolean) : AddPaymentUiState
    data class Error(val message: String) : AddPaymentUiState
}

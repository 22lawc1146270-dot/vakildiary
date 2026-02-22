package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Document

sealed interface DocumentListUiState {
    object Loading : DocumentListUiState
    data class Success(val documents: List<Document>) : DocumentListUiState
    data class Error(val message: String) : DocumentListUiState
}

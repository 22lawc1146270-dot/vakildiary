package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Task

sealed interface OverdueTasksUiState {
    object Loading : OverdueTasksUiState
    data class Success(val tasks: List<Task>) : OverdueTasksUiState
    data class Error(val message: String) : OverdueTasksUiState
}

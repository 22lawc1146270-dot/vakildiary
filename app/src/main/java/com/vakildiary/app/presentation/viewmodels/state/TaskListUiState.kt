package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Task

sealed interface TaskListUiState {
    object Loading : TaskListUiState
    data class Success(val tasks: List<Task>) : TaskListUiState
    data class Error(val message: String) : TaskListUiState
}

package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.Task

sealed interface CalendarUiState {
    object Loading : CalendarUiState
    data class Success(
        val cases: List<Case>,
        val tasks: List<Task>
    ) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}

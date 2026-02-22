package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Meeting

sealed interface MeetingUiState {
    object Loading : MeetingUiState
    data class Success(val meetings: List<Meeting>) : MeetingUiState
    data class Error(val message: String) : MeetingUiState
}

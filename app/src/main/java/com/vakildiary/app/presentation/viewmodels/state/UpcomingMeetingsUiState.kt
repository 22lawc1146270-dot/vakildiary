package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.presentation.viewmodels.UpcomingMeetingItem

sealed interface UpcomingMeetingsUiState {
    object Loading : UpcomingMeetingsUiState
    data class Success(val meetings: List<UpcomingMeetingItem>) : UpcomingMeetingsUiState
    data class Error(val message: String) : UpcomingMeetingsUiState
}

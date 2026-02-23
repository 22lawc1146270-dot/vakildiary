package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Meeting
import com.vakildiary.app.domain.usecase.cases.GetAllCasesUseCase
import com.vakildiary.app.domain.usecase.meeting.GetUpcomingMeetingsUseCase
import com.vakildiary.app.presentation.viewmodels.state.UpcomingMeetingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UpcomingMeetingsViewModel @Inject constructor(
    getUpcomingMeetingsUseCase: GetUpcomingMeetingsUseCase,
    getAllCasesUseCase: GetAllCasesUseCase
) : ViewModel() {

    val uiState: StateFlow<UpcomingMeetingsUiState> = combine(
        getUpcomingMeetingsUseCase(System.currentTimeMillis()),
        getAllCasesUseCase()
    ) { meetingsResult, casesResult ->
        val meetings = (meetingsResult as? Result.Success)?.data.orEmpty()
        val cases = (casesResult as? Result.Success)?.data.orEmpty()
        val caseMap = cases.associateBy { it.caseId }
        val items = meetings.map { meeting ->
            UpcomingMeetingItem(
                meeting = meeting,
                caseName = caseMap[meeting.caseId]?.caseName
            )
        }
        UpcomingMeetingsUiState.Success(items) as UpcomingMeetingsUiState
    }
        .catch { emit(UpcomingMeetingsUiState.Error("Failed to load meetings")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpcomingMeetingsUiState.Loading)
}

data class UpcomingMeetingItem(
    val meeting: Meeting,
    val caseName: String?
)

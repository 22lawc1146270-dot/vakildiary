package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Meeting
import com.vakildiary.app.domain.usecase.meeting.AddMeetingUseCase
import com.vakildiary.app.domain.usecase.meeting.DeleteMeetingUseCase
import com.vakildiary.app.domain.usecase.meeting.GetMeetingsByCaseUseCase
import com.vakildiary.app.presentation.viewmodels.state.MeetingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val getMeetingsByCaseUseCase: GetMeetingsByCaseUseCase,
    private val addMeetingUseCase: AddMeetingUseCase,
    private val deleteMeetingUseCase: DeleteMeetingUseCase
) : ViewModel() {

    fun meetings(caseId: String): StateFlow<MeetingUiState> {
        return getMeetingsByCaseUseCase(caseId)
            .map { result ->
                when (result) {
                    is Result.Success -> MeetingUiState.Success(result.data)
                    is Result.Error -> MeetingUiState.Error(result.message)
                }
            }
            .catch { emit(MeetingUiState.Error("Failed to load meetings")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeetingUiState.Loading)
    }

    fun addMeeting(
        caseId: String,
        clientName: String,
        meetingDate: Long,
        location: String,
        agenda: String,
        notes: String,
        reminderMinutesBefore: Int
    ) {
        viewModelScope.launch {
            val meeting = Meeting(
                meetingId = UUID.randomUUID().toString(),
                caseId = caseId,
                clientName = clientName,
                meetingDate = meetingDate,
                location = location,
                agenda = agenda,
                notes = notes,
                reminderMinutesBefore = reminderMinutesBefore,
                createdAt = System.currentTimeMillis()
            )
            addMeetingUseCase(meeting)
        }
    }

    fun deleteMeeting(meeting: Meeting) {
        viewModelScope.launch {
            deleteMeetingUseCase(meeting)
        }
    }
}

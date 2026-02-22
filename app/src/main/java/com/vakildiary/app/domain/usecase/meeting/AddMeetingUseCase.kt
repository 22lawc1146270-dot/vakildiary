package com.vakildiary.app.domain.usecase.meeting

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Meeting
import com.vakildiary.app.domain.repository.MeetingRepository
import javax.inject.Inject

class AddMeetingUseCase @Inject constructor(
    private val repository: MeetingRepository
) {
    suspend operator fun invoke(meeting: Meeting): Result<Unit> {
        return repository.insertMeeting(meeting)
    }
}

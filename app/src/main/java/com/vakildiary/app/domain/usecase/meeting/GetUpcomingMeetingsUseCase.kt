package com.vakildiary.app.domain.usecase.meeting

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Meeting
import com.vakildiary.app.domain.repository.MeetingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUpcomingMeetingsUseCase @Inject constructor(
    private val repository: MeetingRepository
) {
    operator fun invoke(now: Long): Flow<Result<List<Meeting>>> {
        return repository.getUpcomingMeetings(now)
    }
}

package com.vakildiary.app.domain.usecase.meeting

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Meeting
import com.vakildiary.app.domain.repository.MeetingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMeetingsByCaseUseCase @Inject constructor(
    private val repository: MeetingRepository
) {
    operator fun invoke(caseId: String): Flow<Result<List<Meeting>>> {
        return repository.getMeetingsByCaseId(caseId)
    }
}

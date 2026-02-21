package com.vakildiary.app.domain.usecase.hearing

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.domain.repository.HearingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHearingHistoryUseCase @Inject constructor(
    private val hearingRepository: HearingRepository
) {
    operator fun invoke(caseId: String): Flow<Result<List<HearingHistory>>> {
        return hearingRepository.getHearingsByCaseId(caseId)
    }
}

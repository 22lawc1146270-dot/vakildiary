package com.vakildiary.app.domain.usecase.hearing

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.domain.repository.HearingRepository
import javax.inject.Inject

class AddHearingUseCase @Inject constructor(
    private val hearingRepository: HearingRepository
) {
    suspend operator fun invoke(hearing: HearingHistory): Result<Unit> {
        return hearingRepository.insertHearing(hearing)
    }
}

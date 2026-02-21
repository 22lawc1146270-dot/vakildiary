package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.HearingHistory
import kotlinx.coroutines.flow.Flow

interface HearingRepository {
    suspend fun insertHearing(hearing: HearingHistory): Result<Unit>
    fun getHearingsByCaseId(caseId: String): Flow<Result<List<HearingHistory>>>
    fun getHearingById(hearingId: String): Flow<Result<HearingHistory?>>
}

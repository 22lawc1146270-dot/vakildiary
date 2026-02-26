package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.local.dao.HearingHistoryDao
import com.vakildiary.app.data.local.entities.HearingHistoryEntity
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.domain.repository.HearingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HearingRepositoryImpl @Inject constructor(
    private val hearingHistoryDao: HearingHistoryDao
) : HearingRepository {
    override suspend fun insertHearing(hearing: HearingHistory): Result<Unit> {
        return try {
            hearingHistoryDao.insertHearing(hearing.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to insert hearing", throwable = t)
        }
    }

    override fun getHearingsByCaseId(caseId: String): Flow<Result<List<HearingHistory>>> {
        return hearingHistoryDao.getHearingsByCaseId(caseId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override fun getHearingById(hearingId: String): Flow<Result<HearingHistory?>> {
        return hearingHistoryDao.getHearingById(hearingId).map { entity ->
            Result.Success(entity?.toDomain())
        }
    }

    override suspend fun deleteHearingsByCaseId(caseId: String): Result<Unit> {
        return try {
            hearingHistoryDao.deleteHearingsByCaseId(caseId)
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to delete hearings", throwable = t)
        }
    }

    private fun HearingHistory.toEntity(): HearingHistoryEntity =
        HearingHistoryEntity(
            hearingId = hearingId,
            caseId = caseId,
            hearingDate = hearingDate,
            purpose = purpose,
            outcome = outcome,
            orderDetails = orderDetails,
            nextDateGiven = nextDateGiven,
            adjournmentReason = adjournmentReason,
            voiceNotePath = voiceNotePath,
            createdAt = createdAt
        )

    private fun HearingHistoryEntity.toDomain(): HearingHistory =
        HearingHistory(
            hearingId = hearingId,
            caseId = caseId,
            hearingDate = hearingDate,
            purpose = purpose,
            outcome = outcome,
            orderDetails = orderDetails,
            nextDateGiven = nextDateGiven,
            adjournmentReason = adjournmentReason,
            voiceNotePath = voiceNotePath,
            createdAt = createdAt
        )
}

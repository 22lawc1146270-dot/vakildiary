package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.local.dao.ECourtTrackedCaseDao
import com.vakildiary.app.data.local.entities.ECourtTrackedCaseEntity
import com.vakildiary.app.domain.model.ECourtTrackedCase
import com.vakildiary.app.domain.repository.ECourtTrackedCaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ECourtTrackedCaseRepositoryImpl @Inject constructor(
    private val dao: ECourtTrackedCaseDao
) : ECourtTrackedCaseRepository {
    override fun getAll(): Flow<Result<List<ECourtTrackedCase>>> {
        return dao.getAll().map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun upsert(case: ECourtTrackedCase): Result<Unit> {
        return try {
            dao.upsert(case.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error("Failed to save tracked case", t)
        }
    }

    override suspend fun deleteById(trackId: String): Result<Unit> {
        return try {
            dao.deleteById(trackId)
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error("Failed to delete tracked case", t)
        }
    }

    private fun ECourtTrackedCase.toEntity(): ECourtTrackedCaseEntity {
        return ECourtTrackedCaseEntity(
            trackId = trackId,
            caseTitle = caseTitle,
            caseNumber = caseNumber,
            year = year,
            courtName = courtName,
            courtType = courtType,
            parties = parties,
            stage = stage,
            nextHearingDate = nextHearingDate,
            caseDetails = caseDetails.joinToString(SECTION_DELIMITER),
            caseStatus = caseStatus.joinToString(SECTION_DELIMITER),
            petitionerAdvocate = petitionerAdvocate.joinToString(SECTION_DELIMITER),
            respondentAdvocate = respondentAdvocate.joinToString(SECTION_DELIMITER),
            acts = acts.joinToString(SECTION_DELIMITER),
            caseHistory = caseHistory.joinToString(SECTION_DELIMITER),
            transferDetails = transferDetails.joinToString(SECTION_DELIMITER),
            createdAt = createdAt
        )
    }

    private fun ECourtTrackedCaseEntity.toDomain(): ECourtTrackedCase {
        return ECourtTrackedCase(
            trackId = trackId,
            caseTitle = caseTitle,
            caseNumber = caseNumber,
            year = year,
            courtName = courtName,
            courtType = courtType,
            parties = parties,
            stage = stage,
            nextHearingDate = nextHearingDate,
            caseDetails = splitSection(caseDetails),
            caseStatus = splitSection(caseStatus),
            petitionerAdvocate = splitSection(petitionerAdvocate),
            respondentAdvocate = splitSection(respondentAdvocate),
            acts = splitSection(acts),
            caseHistory = splitSection(caseHistory),
            transferDetails = splitSection(transferDetails),
            createdAt = createdAt
        )
    }

    private fun splitSection(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(SECTION_DELIMITER).map { it.trim() }.filter { it.isNotBlank() }
    }

    companion object {
        private const val SECTION_DELIMITER = "\n"
    }
}

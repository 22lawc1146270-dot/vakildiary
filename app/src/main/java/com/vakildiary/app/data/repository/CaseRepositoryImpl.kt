package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.entities.CaseEntity
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.repository.CaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CaseRepositoryImpl @Inject constructor(
    private val caseDao: CaseDao
) : CaseRepository {
    override suspend fun insertCase(case: Case): Result<Unit> {
        return try {
            caseDao.insertCase(case.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to insert case", throwable = t)
        }
    }

    override suspend fun updateCase(case: Case): Result<Unit> {
        return try {
            caseDao.updateCase(case.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to update case", throwable = t)
        }
    }

    override fun getCaseById(caseId: String): Flow<Result<Case?>> {
        return caseDao.getCaseById(caseId).map { entity ->
            Result.Success(entity?.toDomain())
        }
    }

    override fun getAllActiveCases(): Flow<Result<List<Case>>> {
        return caseDao.getAllActiveCases().map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override fun searchCases(query: String): Flow<Result<List<Case>>> {
        return caseDao.searchCases(query).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun archiveCase(caseId: String): Result<Unit> {
        return try {
            caseDao.archiveCase(caseId)
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to archive case", throwable = t)
        }
    }

    private fun Case.toEntity(): CaseEntity =
        CaseEntity(
            caseId = caseId,
            caseName = caseName,
            caseNumber = caseNumber,
            courtName = courtName,
            courtType = courtType,
            clientName = clientName,
            clientPhone = clientPhone,
            clientEmail = clientEmail,
            oppositeParty = oppositeParty,
            caseType = caseType,
            caseStage = caseStage,
            assignedJudge = assignedJudge,
            firNumber = firNumber,
            actsAndSections = actsAndSections,
            nextHearingDate = nextHearingDate,
            totalAgreedFees = totalAgreedFees,
            isECourtTracked = isECourtTracked,
            eCourtCaseId = eCourtCaseId,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isArchived = isArchived
        )

    private fun CaseEntity.toDomain(): Case =
        Case(
            caseId = caseId,
            caseName = caseName,
            caseNumber = caseNumber,
            courtName = courtName,
            courtType = courtType,
            clientName = clientName,
            clientPhone = clientPhone,
            clientEmail = clientEmail,
            oppositeParty = oppositeParty,
            caseType = caseType,
            caseStage = caseStage,
            assignedJudge = assignedJudge,
            firNumber = firNumber,
            actsAndSections = actsAndSections,
            nextHearingDate = nextHearingDate,
            totalAgreedFees = totalAgreedFees,
            isECourtTracked = isECourtTracked,
            eCourtCaseId = eCourtCaseId,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isArchived = isArchived
        )
}

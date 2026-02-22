package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Case
import kotlinx.coroutines.flow.Flow

interface CaseRepository {
    suspend fun insertCase(case: Case): Result<Unit>
    suspend fun updateCase(case: Case): Result<Unit>
    fun getCaseById(caseId: String): Flow<Result<Case?>>
    suspend fun getCaseByNumber(caseNumber: String): Result<Case?>
    fun getAllActiveCases(): Flow<Result<List<Case>>>
    fun searchCases(query: String): Flow<Result<List<Case>>>
    suspend fun archiveCase(caseId: String): Result<Unit>
}

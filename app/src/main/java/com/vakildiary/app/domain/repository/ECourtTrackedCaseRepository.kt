package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.ECourtTrackedCase
import kotlinx.coroutines.flow.Flow

interface ECourtTrackedCaseRepository {
    fun getAll(): Flow<Result<List<ECourtTrackedCase>>>
    suspend fun upsert(case: ECourtTrackedCase): Result<Unit>
    suspend fun deleteById(trackId: String): Result<Unit>
}

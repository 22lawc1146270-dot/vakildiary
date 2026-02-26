package com.vakildiary.app.domain.usecase.ecourt

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.ECourtTrackedCase
import com.vakildiary.app.domain.repository.ECourtTrackedCaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetECourtTrackedCasesUseCase @Inject constructor(
    private val repository: ECourtTrackedCaseRepository
) {
    operator fun invoke(): Flow<Result<List<ECourtTrackedCase>>> = repository.getAll()
}

class UpsertECourtTrackedCaseUseCase @Inject constructor(
    private val repository: ECourtTrackedCaseRepository
) {
    suspend operator fun invoke(case: ECourtTrackedCase): Result<Unit> = repository.upsert(case)
}

class DeleteECourtTrackedCaseUseCase @Inject constructor(
    private val repository: ECourtTrackedCaseRepository
) {
    suspend operator fun invoke(trackId: String): Result<Unit> = repository.deleteById(trackId)
}

package com.vakildiary.app.domain.usecase.cases

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.CaseRepository
import javax.inject.Inject

class ArchiveCaseUseCase @Inject constructor(
    private val caseRepository: CaseRepository
) {
    suspend operator fun invoke(caseId: String): Result<Unit> {
        return caseRepository.archiveCase(caseId)
    }
}

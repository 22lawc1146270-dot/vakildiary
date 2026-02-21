package com.vakildiary.app.domain.usecase.cases

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.repository.CaseRepository
import javax.inject.Inject

class AddCaseUseCase @Inject constructor(
    private val caseRepository: CaseRepository
) {
    suspend operator fun invoke(case: Case): Result<Unit> {
        return caseRepository.insertCase(case)
    }
}

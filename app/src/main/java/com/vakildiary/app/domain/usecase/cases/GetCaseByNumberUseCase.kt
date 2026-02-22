package com.vakildiary.app.domain.usecase.cases

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.repository.CaseRepository
import javax.inject.Inject

class GetCaseByNumberUseCase @Inject constructor(
    private val repository: CaseRepository
) {
    suspend operator fun invoke(caseNumber: String): Result<Case?> {
        return repository.getCaseByNumber(caseNumber)
    }
}

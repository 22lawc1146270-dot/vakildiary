package com.vakildiary.app.domain.usecase.cases

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.repository.CaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchCasesUseCase @Inject constructor(
    private val caseRepository: CaseRepository
) {
    operator fun invoke(query: String): Flow<Result<List<Case>>> {
        return caseRepository.searchCases(query)
    }
}

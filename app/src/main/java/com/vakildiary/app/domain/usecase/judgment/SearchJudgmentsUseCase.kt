package com.vakildiary.app.domain.usecase.judgment

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.domain.repository.SCJudgmentRepository
import javax.inject.Inject

class SearchJudgmentsUseCase @Inject constructor(
    private val repository: SCJudgmentRepository
) {
    suspend operator fun invoke(
        query: String,
        year: String
    ): Result<List<JudgmentSearchResult>> {
        return repository.searchJudgments(query, year)
    }
}

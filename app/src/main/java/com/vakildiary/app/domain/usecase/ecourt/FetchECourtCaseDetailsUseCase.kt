package com.vakildiary.app.domain.usecase.ecourt

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.ECourtRepository
import javax.inject.Inject

class FetchECourtCaseDetailsUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(
        token: String,
        detailLink: String
    ): Result<String> {
        return repository.fetchCaseDetails(token, detailLink)
    }
}

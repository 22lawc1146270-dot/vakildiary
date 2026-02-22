package com.vakildiary.app.domain.usecase.ecourt

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.ECourtRepository
import javax.inject.Inject

class SearchECourtUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(
        stateCode: String,
        districtCode: String,
        courtCode: String,
        caseType: String,
        caseNumber: String,
        year: String,
        captcha: String,
        csrfMagic: String? = null
    ): Result<String> {
        return repository.searchCaseByNumber(
            stateCode = stateCode,
            districtCode = districtCode,
            courtCode = courtCode,
            caseType = caseType,
            caseNumber = caseNumber,
            year = year,
            captcha = captcha,
            csrfMagic = csrfMagic
        )
    }
}

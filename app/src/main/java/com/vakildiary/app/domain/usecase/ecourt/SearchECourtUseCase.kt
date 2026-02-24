package com.vakildiary.app.domain.usecase.ecourt

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.ECourtSearchResult
import com.vakildiary.app.domain.repository.ECourtRepository
import javax.inject.Inject

class SearchECourtUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(
        token: String,
        stateCode: String,
        districtCode: String,
        courtComplexCode: String,
        establishmentCode: String?,
        caseType: String,
        caseNumber: String,
        year: String,
        captcha: String
    ): Result<ECourtSearchResult> {
        return repository.searchCaseByNumber(
            token = token,
            stateCode = stateCode,
            districtCode = districtCode,
            courtComplexCode = courtComplexCode,
            establishmentCode = establishmentCode,
            caseType = caseType,
            caseNumber = caseNumber,
            year = year,
            captcha = captcha
        )
    }
}

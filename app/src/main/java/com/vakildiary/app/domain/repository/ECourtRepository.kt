package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result

interface ECourtRepository {
    suspend fun searchCaseByNumber(
        stateCode: String,
        districtCode: String,
        courtCode: String,
        caseType: String,
        caseNumber: String,
        year: String,
        captcha: String,
        csrfMagic: String? = null
    ): Result<String>
}

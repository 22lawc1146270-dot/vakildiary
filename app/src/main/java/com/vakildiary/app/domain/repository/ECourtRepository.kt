package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.ECourtCaptcha
import com.vakildiary.app.domain.model.ECourtComplexOption
import com.vakildiary.app.domain.model.ECourtOption
import com.vakildiary.app.domain.model.ECourtSearchResult
import com.vakildiary.app.domain.model.ECourtSession
import com.vakildiary.app.domain.model.ECourtTokenResult

interface ECourtRepository {
    suspend fun fetchSession(): Result<ECourtSession>

    suspend fun fetchDistricts(
        token: String,
        stateCode: String
    ): Result<ECourtTokenResult<List<ECourtOption>>>

    suspend fun fetchCourtComplexes(
        token: String,
        stateCode: String,
        districtCode: String
    ): Result<ECourtTokenResult<List<ECourtComplexOption>>>

    suspend fun fetchEstablishments(
        token: String,
        stateCode: String,
        districtCode: String,
        courtComplexCode: String
    ): Result<ECourtTokenResult<List<ECourtOption>>>

    suspend fun fetchCaseTypes(
        token: String,
        stateCode: String,
        districtCode: String,
        courtComplexCode: String,
        establishmentCode: String?
    ): Result<ECourtTokenResult<List<ECourtOption>>>

    suspend fun fetchCaptcha(token: String): Result<ECourtCaptcha>

    suspend fun searchCaseByNumber(
        token: String,
        stateCode: String,
        districtCode: String,
        courtComplexCode: String,
        establishmentCode: String?,
        caseType: String,
        caseNumber: String,
        year: String,
        captcha: String
    ): Result<ECourtSearchResult>

    suspend fun fetchCaseDetails(
        token: String,
        detailLink: String
    ): Result<String>
}

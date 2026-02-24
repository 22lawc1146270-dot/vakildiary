package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.ecourt.ECourtHtmlParser
import com.vakildiary.app.data.remote.ECourtApiService
import com.vakildiary.app.domain.model.ECourtCaptcha
import com.vakildiary.app.domain.model.ECourtComplexOption
import com.vakildiary.app.domain.model.ECourtOption
import com.vakildiary.app.domain.model.ECourtSearchResult
import com.vakildiary.app.domain.model.ECourtSession
import com.vakildiary.app.domain.model.ECourtTokenResult
import com.vakildiary.app.domain.repository.ECourtRepository
import javax.inject.Inject

class ECourtRepositoryImpl @Inject constructor(
    private val apiService: ECourtApiService
) : ECourtRepository {

    override suspend fun fetchSession(): Result<ECourtSession> {
        return try {
            val response = apiService.getCaseStatusPage()
            if (!response.isSuccessful) {
                return Result.Error("Failed to load eCourt session: ${response.code()}")
            }
            val html = response.body()?.string().orEmpty()
            val token = ECourtHtmlParser.parseAppToken(html)
                ?: return Result.Error("eCourt session token missing")
            val states = ECourtHtmlParser.parseStateOptions(html)
            Result.Success(ECourtSession(token = token, states = states))
        } catch (t: Throwable) {
            Result.Error("Failed to load eCourt session", t)
        }
    }

    override suspend fun fetchDistricts(
        token: String,
        stateCode: String
    ): Result<ECourtTokenResult<List<ECourtOption>>> {
        return try {
            val response = apiService.fillDistrict(token = token, stateCode = stateCode)
            if (response.status != 1) {
                return Result.Error(response.errorMessage ?: "Failed to load districts")
            }
            val newToken = response.appToken ?: token
            val list = ECourtHtmlParser.parseOptions(response.districtList.orEmpty())
            Result.Success(ECourtTokenResult(token = newToken, data = list))
        } catch (t: Throwable) {
            Result.Error("Failed to load districts", t)
        }
    }

    override suspend fun fetchCourtComplexes(
        token: String,
        stateCode: String,
        districtCode: String
    ): Result<ECourtTokenResult<List<ECourtComplexOption>>> {
        return try {
            val response = apiService.fillCourtComplex(
                token = token,
                stateCode = stateCode,
                districtCode = districtCode
            )
            if (response.status != 1) {
                return Result.Error(response.errorMessage ?: "Failed to load courts")
            }
            val newToken = response.appToken ?: token
            val list = ECourtHtmlParser.parseComplexOptions(response.complexList.orEmpty())
            Result.Success(ECourtTokenResult(token = newToken, data = list))
        } catch (t: Throwable) {
            Result.Error("Failed to load courts", t)
        }
    }

    override suspend fun fetchEstablishments(
        token: String,
        stateCode: String,
        districtCode: String,
        courtComplexCode: String
    ): Result<ECourtTokenResult<List<ECourtOption>>> {
        return try {
            val response = apiService.fillCourtEstablishment(
                token = token,
                stateCode = stateCode,
                districtCode = districtCode,
                courtComplexCode = courtComplexCode
            )
            if (response.status != 1) {
                return Result.Error(response.errorMessage ?: "Failed to load establishments")
            }
            val newToken = response.appToken ?: token
            val list = ECourtHtmlParser.parseOptions(response.establishmentList.orEmpty())
            Result.Success(ECourtTokenResult(token = newToken, data = list))
        } catch (t: Throwable) {
            Result.Error("Failed to load establishments", t)
        }
    }

    override suspend fun fetchCaseTypes(
        token: String,
        stateCode: String,
        districtCode: String,
        courtComplexCode: String,
        establishmentCode: String?
    ): Result<ECourtTokenResult<List<ECourtOption>>> {
        return try {
            val response = apiService.fillCaseType(
                token = token,
                stateCode = stateCode,
                districtCode = districtCode,
                courtComplexCode = courtComplexCode,
                establishmentCode = establishmentCode.orEmpty(),
                searchType = "c_no"
            )
            if (response.status != 1) {
                return Result.Error(response.errorMessage ?: "Failed to load case types")
            }
            val newToken = response.appToken ?: token
            val list = ECourtHtmlParser.parseOptions(response.caseTypeList.orEmpty())
            Result.Success(ECourtTokenResult(token = newToken, data = list))
        } catch (t: Throwable) {
            Result.Error("Failed to load case types", t)
        }
    }

    override suspend fun fetchCaptcha(token: String): Result<ECourtCaptcha> {
        return try {
            val response = apiService.getCaptcha(token = token)
            val captchaHtml = response.captchaHtml.orEmpty()
            val imageUrl = ECourtHtmlParser.parseCaptchaImageUrl(captchaHtml)
                ?: return Result.Error(response.errorMessage ?: "Failed to load captcha")
            val newToken = response.appToken ?: token
            Result.Success(ECourtCaptcha(token = newToken, imageUrl = imageUrl))
        } catch (t: Throwable) {
            Result.Error("Failed to load captcha", t)
        }
    }

    override suspend fun searchCaseByNumber(
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
        val params = mutableMapOf(
            "state_code" to stateCode,
            "dist_code" to districtCode,
            "court_complex_code" to courtComplexCode,
            "est_code" to establishmentCode.orEmpty(),
            "case_type" to caseType,
            "case_no" to caseNumber,
            "search_case_no" to caseNumber,
            "rgyear" to year,
            "case_captcha_code" to captcha
        )

        return try {
            val response = apiService.submitCaseNo(
                token = token,
                params = params
            )
            if (response.status != 1) {
                return Result.Error(response.errorMessage ?: "eCourt search failed")
            }
            val caseHtml = response.caseData.orEmpty()
            val imageUrl = ECourtHtmlParser.parseCaptchaImageUrl(response.captchaHtml.orEmpty())
            val newToken = response.appToken ?: token
            Result.Success(
                ECourtSearchResult(
                    token = newToken,
                    caseHtml = caseHtml,
                    captchaImageUrl = imageUrl
                )
            )
        } catch (t: Throwable) {
            Result.Error("eCourt search failed", t)
        }
    }
}

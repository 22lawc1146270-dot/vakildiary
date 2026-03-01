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
    private companion object {
        const val ECOURT_BASE_URL = "https://services.ecourts.gov.in/ecourtindia_v6/"
    }

    override suspend fun fetchSession(): Result<ECourtSession> {
        return try {
            val response = apiService.getCaseStatusPage()
            if (!response.isSuccessful) {
                return Result.Error("Failed to load eCourt session: ${response.code()}")
            }
            val html = response.body()?.string().orEmpty()
            val token = ECourtHtmlParser.parseAppToken(html).orEmpty()
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
            val imageBytes = fetchCaptchaBytes(imageUrl)
                ?: return Result.Error("Failed to load captcha image")
            val newToken = response.appToken ?: token
            Result.Success(
                ECourtCaptcha(
                    token = newToken,
                    imageUrl = imageUrl,
                    imageBytes = imageBytes
                )
            )
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
            val imageBytes = imageUrl?.let { fetchCaptchaBytes(it) }
            val newToken = response.appToken ?: token
            Result.Success(
                ECourtSearchResult(
                    token = newToken,
                    caseHtml = caseHtml,
                    captchaImageUrl = imageUrl,
                    captchaImageBytes = imageBytes
                )
            )
        } catch (t: Throwable) {
            Result.Error("eCourt search failed", t)
        }
    }

    override suspend fun fetchCaseDetails(
        token: String,
        detailLink: String
    ): Result<String> {
        return try {
            val resolvedUrl = resolveCaseDetailUrl(detailLink, token)
                ?: return Result.Error("Case detail link missing")
            val response = apiService.fetchCaseDetails(resolvedUrl)
            if (!response.isSuccessful) {
                return Result.Error("Failed to load case details: ${response.code()}")
            }
            val html = response.body()?.string().orEmpty()
            if (html.isBlank()) {
                return Result.Error("Case details unavailable")
            }
            Result.Success(html)
        } catch (t: Throwable) {
            Result.Error("Failed to load case details", t)
        }
    }

    private suspend fun fetchCaptchaBytes(imageUrl: String): ByteArray? {
        val response = apiService.fetchCaptchaImage(imageUrl)
        if (!response.isSuccessful) return null
        return response.body()?.bytes()
    }

    private fun resolveCaseDetailUrl(detailLink: String, token: String): String? {
        val cleaned = detailLink.replace("&amp;", "&").trim()
        if (cleaned.isBlank()) return null
        val baseUrl = when {
            cleaned.startsWith("http", ignoreCase = true) -> cleaned
            cleaned.startsWith("/") -> "https://services.ecourts.gov.in${cleaned}"
            cleaned.startsWith("?") -> "${ECOURT_BASE_URL}${cleaned}"
            else -> "${ECOURT_BASE_URL}${cleaned}"
        }
        if (token.isBlank() || baseUrl.contains("app_token=")) return baseUrl
        return if (baseUrl.contains("?")) {
            "$baseUrl&app_token=$token"
        } else {
            "$baseUrl?app_token=$token"
        }
    }
}

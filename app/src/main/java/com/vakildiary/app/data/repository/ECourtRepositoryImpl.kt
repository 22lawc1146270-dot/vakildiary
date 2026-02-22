package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.remote.ECourtApiService
import com.vakildiary.app.domain.repository.ECourtRepository
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class ECourtRepositoryImpl @Inject constructor(
    private val apiService: ECourtApiService
) : ECourtRepository {

    override suspend fun searchCaseByNumber(
        stateCode: String,
        districtCode: String,
        courtCode: String,
        caseType: String,
        caseNumber: String,
        year: String,
        captcha: String,
        csrfMagic: String?
    ): Result<String> {
        val params = buildSearchParams(
            stateCode = stateCode,
            districtCode = districtCode,
            courtCode = courtCode,
            caseType = caseType,
            caseNumber = caseNumber,
            year = year,
            captcha = captcha,
            csrfMagic = csrfMagic
        )

        return try {
            val response: Response<ResponseBody> = apiService.searchCaseByNumber(params)
            if (response.isSuccessful) {
                Result.Success(response.body()?.string().orEmpty())
            } else {
                Result.Error("eCourt search failed: ${response.code()}")
            }
        } catch (t: Throwable) {
            Result.Error("eCourt search failed", t)
        }
    }

    private fun buildSearchParams(
        stateCode: String,
        districtCode: String,
        courtCode: String,
        caseType: String,
        caseNumber: String,
        year: String,
        captcha: String,
        csrfMagic: String?
    ): Map<String, String> {
        val params = mutableMapOf(
            "action_code" to "showRecords",
            "state_code" to stateCode,
            "dist_code" to districtCode,
            "court_code" to courtCode,
            "case_type" to caseType,
            "case_no" to caseNumber,
            "rgyear" to year,
            "caseNoType" to "new",
            "displayOldCaseNo" to "NO",
            "captcha" to captcha
        )

        val magic = csrfMagic ?: DEFAULT_CSRF_MAGIC
        params["__csrf_magic"] = magic
        return params
    }

    companion object {
        // From OpenJustice docs; this may change if the upstream site rotates tokens.
        private const val DEFAULT_CSRF_MAGIC = "2f8b1b05c22ec0e5e3c3f0d3a0d9f8a8"
    }
}

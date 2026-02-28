package com.vakildiary.app.data.remote.reportable

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ReportableBackendService {
    @GET("api/reportable/form")
    suspend fun fetchForm(
        @Query("caseNumber") caseNumber: String?,
        @Query("year") year: String?
    ): ReportableFormResponseDto

    @POST("api/reportable/submit")
    suspend fun submitForm(
        @Body request: ReportableSubmitRequestDto
    ): ReportableSubmitResponseDto

    @Streaming
    @GET
    suspend fun downloadReportable(
        @Url downloadUrl: String
    ): ResponseBody
}

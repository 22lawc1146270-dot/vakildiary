package com.vakildiary.app.data.remote

import com.vakildiary.app.data.remote.ecourt.ECourtCaseSearchResponse
import com.vakildiary.app.data.remote.ecourt.ECourtCaptchaResponse
import com.vakildiary.app.data.remote.ecourt.ECourtListResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface ECourtApiService {
    @GET("?p=casestatus/index")
    suspend fun getCaseStatusPage(): Response<ResponseBody>

    @POST("?p=casestatus/getCaptcha")
    suspend fun getCaptcha(
        @Query("app_token") token: String,
        @Query("ajax_req") ajaxReq: Boolean = true
    ): ECourtCaptchaResponse

    @FormUrlEncoded
    @POST("?p=casestatus/fillDistrict")
    suspend fun fillDistrict(
        @Query("app_token") token: String,
        @Query("ajax_req") ajaxReq: Boolean = true,
        @Field("state_code") stateCode: String
    ): ECourtListResponse

    @FormUrlEncoded
    @POST("?p=casestatus/fillcomplex")
    suspend fun fillCourtComplex(
        @Query("app_token") token: String,
        @Query("ajax_req") ajaxReq: Boolean = true,
        @Field("state_code") stateCode: String,
        @Field("dist_code") districtCode: String
    ): ECourtListResponse

    @FormUrlEncoded
    @POST("?p=casestatus/fillCourtEstablishment")
    suspend fun fillCourtEstablishment(
        @Query("app_token") token: String,
        @Query("ajax_req") ajaxReq: Boolean = true,
        @Field("state_code") stateCode: String,
        @Field("dist_code") districtCode: String,
        @Field("court_complex_code") courtComplexCode: String
    ): ECourtListResponse

    @FormUrlEncoded
    @POST("?p=casestatus/fillCaseType")
    suspend fun fillCaseType(
        @Query("app_token") token: String,
        @Query("ajax_req") ajaxReq: Boolean = true,
        @Field("state_code") stateCode: String,
        @Field("dist_code") districtCode: String,
        @Field("court_complex_code") courtComplexCode: String,
        @Field("est_code") establishmentCode: String,
        @Field("search_type") searchType: String
    ): ECourtListResponse

    @FormUrlEncoded
    @POST("?p=casestatus/submitCaseNo")
    suspend fun submitCaseNo(
        @Query("app_token") token: String,
        @Query("ajax_req") ajaxReq: Boolean = true,
        @FieldMap params: Map<String, String>
    ): ECourtCaseSearchResponse

    @GET
    suspend fun fetchCaptchaImage(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun fetchCaseDetails(@Url url: String): Response<ResponseBody>
}

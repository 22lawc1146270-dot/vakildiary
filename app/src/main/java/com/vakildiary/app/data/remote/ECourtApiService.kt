package com.vakildiary.app.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ECourtApiService {
    @FormUrlEncoded
    @POST("cases/case_no_qry.php")
    suspend fun searchCaseByNumber(
        @FieldMap params: Map<String, String>
    ): Response<ResponseBody>
}

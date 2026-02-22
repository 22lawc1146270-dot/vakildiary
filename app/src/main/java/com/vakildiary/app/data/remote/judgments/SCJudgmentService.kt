package com.vakildiary.app.data.remote.judgments

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface SCJudgmentService {
    @GET("metadata/tar/year={year}/metadata.index.json")
    suspend fun getMetadataIndex(
        @Path("year") year: String
    ): MetadataIndexDto

    @Streaming
    @GET
    suspend fun downloadJudgment(
        @Url judgmentPath: String
    ): ResponseBody
}

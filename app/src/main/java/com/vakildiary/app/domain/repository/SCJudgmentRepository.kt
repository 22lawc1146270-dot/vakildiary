package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import java.io.InputStream

interface SCJudgmentRepository {
    suspend fun searchJudgments(
        query: String,
        year: String
    ): Result<List<JudgmentSearchResult>>

    suspend fun downloadJudgment(
        judgmentId: String
    ): Result<JudgmentDownload>
}

data class JudgmentSearchResult(
    val judgmentId: String,
    val title: String
)

data class JudgmentDownload(
    val fileName: String,
    val mimeType: String,
    val inputStream: InputStream
)

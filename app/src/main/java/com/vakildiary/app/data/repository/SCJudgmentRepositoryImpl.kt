package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.remote.judgments.SCJudgmentService
import com.vakildiary.app.domain.repository.JudgmentDownload
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.domain.repository.SCJudgmentRepository
import javax.inject.Inject

class SCJudgmentRepositoryImpl @Inject constructor(
    private val service: SCJudgmentService
) : SCJudgmentRepository {

    override suspend fun searchJudgments(
        query: String,
        year: String
    ): Result<List<JudgmentSearchResult>> {
        return try {
            val index = service.getMetadataIndex(year)
            val allFiles = index.parts.flatMap { it.files }
            val filtered = if (query.isBlank()) {
                allFiles
            } else {
                allFiles.filter { it.contains(query, ignoreCase = true) }
            }

            val results = filtered.map { fileName ->
                JudgmentSearchResult(
                    judgmentId = fileName,
                    title = fileName
                )
            }
            Result.Success(results)
        } catch (t: Throwable) {
            Result.Error("Judgment search failed", t)
        }
    }

    override suspend fun downloadJudgment(
        judgmentId: String
    ): Result<JudgmentDownload> {
        return try {
            val body = service.downloadJudgment(judgmentId)
            val fileName = buildFileName(judgmentId)
            val mimeType = body.contentType()?.toString() ?: "application/pdf"
            Result.Success(
                JudgmentDownload(
                    fileName = fileName,
                    mimeType = mimeType,
                    inputStream = body.byteStream()
                )
            )
        } catch (t: Throwable) {
            Result.Error("Failed to download judgment", t)
        }
    }

    private fun buildFileName(judgmentId: String): String {
        val rawName = judgmentId.substringAfterLast('/').ifBlank { "judgment_${System.currentTimeMillis()}.pdf" }
        return if (rawName.contains('.')) rawName else "$rawName.pdf"
    }
}

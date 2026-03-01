package com.vakildiary.app.domain.usecase.judgment

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.core.DocumentTags
import com.vakildiary.app.domain.repository.SCJudgmentRepository
import com.vakildiary.app.domain.usecase.documents.AttachDocumentUseCase
import javax.inject.Inject

class DownloadJudgmentUseCase @Inject constructor(
    private val repository: SCJudgmentRepository,
    private val attachDocumentUseCase: AttachDocumentUseCase
) {
    suspend operator fun invoke(
        judgment: JudgmentSearchResult,
        year: String
    ): Result<Document> {
        val fileName = buildCitationFileName(judgment)
        val tags = DocumentTags.buildJudgmentTags(
            judgmentId = judgment.judgmentId,
            year = year,
            caseNumber = judgment.caseNumber,
            reportable = false,
            petitioner = judgment.petitioner,
            judgmentDate = judgment.dateOfJudgment?.toString()
        )
        return when (val result = repository.downloadJudgment(judgment.judgmentId)) {
            is Result.Success -> {
                val download = result.data
                attachDocumentUseCase(
                    caseId = null,
                    fileName = fileName,
                    mimeType = download.mimeType,
                    inputStreamProvider = { download.inputStream },
                    isScanned = false,
                    tags = tags
                )
            }
            is Result.Error -> Result.Error(result.message, result.throwable)
        }
    }

    private fun buildCitationFileName(item: JudgmentSearchResult): String {
        val p = item.petitioner?.trim().orEmpty()
        val r = item.respondent?.trim().orEmpty()
        val c = item.caseNumber?.trim().orEmpty()
        
        val baseName = buildString {
            if (p.isNotBlank() && r.isNotBlank()) {
                append(p)
                append(" v. ")
                append(r)
            } else if (p.isNotBlank()) {
                append(p)
            } else if (r.isNotBlank()) {
                append(r)
            }
            
            if (c.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(c)
            }
            
            if (isEmpty()) {
                append(item.title)
            }
        }

        return "$baseName.pdf"
    }
}

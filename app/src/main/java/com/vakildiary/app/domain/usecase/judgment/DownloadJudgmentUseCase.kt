package com.vakildiary.app.domain.usecase.judgment

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.domain.repository.SCJudgmentRepository
import com.vakildiary.app.domain.usecase.documents.AttachDocumentUseCase
import javax.inject.Inject

class DownloadJudgmentUseCase @Inject constructor(
    private val repository: SCJudgmentRepository,
    private val attachDocumentUseCase: AttachDocumentUseCase
) {
    suspend operator fun invoke(
        judgment: JudgmentSearchResult,
        caseId: String?
    ): Result<Unit> {
        return when (val result = repository.downloadJudgment(judgment.judgmentId)) {
            is Result.Success -> {
                val download = result.data
                attachDocumentUseCase(
                    caseId = caseId,
                    fileName = download.fileName,
                    mimeType = download.mimeType,
                    inputStreamProvider = { download.inputStream },
                    isScanned = false,
                    tags = "judgment"
                )
            }
            is Result.Error -> Result.Error(result.message, result.throwable)
        }
    }
}

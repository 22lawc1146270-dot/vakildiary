package com.vakildiary.app.domain.usecase.documents

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import java.io.InputStream
import javax.inject.Inject

class SaveScannedDocumentUseCase @Inject constructor(
    private val attachDocumentUseCase: AttachDocumentUseCase
) {
    suspend operator fun invoke(
        caseId: String?,
        fileName: String,
        inputStreamProvider: () -> InputStream
    ): Result<Document> {
        return attachDocumentUseCase(
            caseId = caseId,
            fileName = fileName,
            mimeType = "application/pdf",
            inputStreamProvider = inputStreamProvider,
            isScanned = true,
            tags = "scanned"
        )
    }
}

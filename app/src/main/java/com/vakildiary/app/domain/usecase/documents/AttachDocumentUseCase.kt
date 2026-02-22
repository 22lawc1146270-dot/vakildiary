package com.vakildiary.app.domain.usecase.documents

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.DocumentRepository
import com.vakildiary.app.domain.storage.DocumentStorage
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class AttachDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository,
    private val storage: DocumentStorage
) {
    suspend operator fun invoke(
        caseId: String?,
        fileName: String,
        mimeType: String,
        inputStreamProvider: () -> InputStream,
        isScanned: Boolean,
        tags: String = ""
    ): Result<Unit> {
        val stored = when (val result = storage.saveDocument(
            caseId = caseId,
            fileName = fileName,
            mimeType = mimeType,
            inputStreamProvider = inputStreamProvider,
            isScanned = isScanned,
            tags = tags
        )) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }

        val document = Document(
            documentId = UUID.randomUUID().toString(),
            caseId = caseId,
            fileName = stored.fileName,
            filePath = stored.filePath,
            fileType = stored.fileType,
            fileSizeBytes = stored.fileSizeBytes,
            isScanned = stored.isScanned,
            thumbnailPath = stored.thumbnailPath,
            tags = stored.tags,
            createdAt = System.currentTimeMillis()
        )

        return when (val insert = repository.insertDocument(document)) {
            is Result.Success -> insert
            is Result.Error -> {
                storage.deleteFile(stored.filePath)
                insert
            }
        }
    }
}

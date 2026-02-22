package com.vakildiary.app.domain.usecase.documents

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.DocumentRepository
import com.vakildiary.app.domain.storage.DocumentStorage
import javax.inject.Inject

class DeleteDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository,
    private val storage: DocumentStorage
) {
    suspend operator fun invoke(document: Document): Result<Unit> {
        val deleteFileResult = storage.deleteFile(document.filePath)
        return when (val deleteDbResult = repository.deleteDocument(document)) {
            is Result.Success -> deleteDbResult
            is Result.Error -> deleteDbResult
        }.also {
            if (deleteFileResult is Result.Error && it is Result.Success) {
                // File delete failed but DB delete succeeded; keep success to avoid blocking.
            }
        }
    }
}

package com.vakildiary.app.domain.usecase.documents

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.DocumentRepository
import com.vakildiary.app.domain.storage.DocumentStorage
import javax.inject.Inject

class MoveDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository,
    private val storage: DocumentStorage
) {
    suspend operator fun invoke(document: Document, newCaseId: String?): Result<Unit> {
        val newPath = when (val result = storage.moveDocument(document.filePath, newCaseId)) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }

        val updated = document.copy(
            caseId = newCaseId,
            filePath = newPath
        )
        return repository.updateDocument(updated)
    }
}

package com.vakildiary.app.domain.usecase.documents

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDocumentsByCaseIdUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(caseId: String?): Flow<Result<List<Document>>> {
        return repository.getDocumentsByCaseId(caseId)
    }
}

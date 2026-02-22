package com.vakildiary.app.domain.usecase.documents

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(): Flow<Result<List<Document>>> {
        return repository.getAllDocuments()
    }
}

package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    suspend fun insertDocument(document: Document): Result<Unit>
    fun getDocumentsByCaseId(caseId: String?): Flow<Result<List<Document>>>
    suspend fun deleteDocument(document: Document): Result<Unit>
}

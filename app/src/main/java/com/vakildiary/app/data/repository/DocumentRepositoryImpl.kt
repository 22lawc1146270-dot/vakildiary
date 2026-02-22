package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.local.dao.DocumentDao
import com.vakildiary.app.data.local.entities.DocumentEntity
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao
) : DocumentRepository {
    override suspend fun insertDocument(document: Document): Result<Unit> {
        return try {
            documentDao.insertDocument(document.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to insert document", throwable = t)
        }
    }

    override fun getDocumentsByCaseId(caseId: String?): Flow<Result<List<Document>>> {
        return documentDao.getDocumentsByCaseId(caseId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override fun getAllDocuments(): Flow<Result<List<Document>>> {
        return documentDao.getAllDocuments().map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun deleteDocument(document: Document): Result<Unit> {
        return try {
            documentDao.deleteDocument(document.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to delete document", throwable = t)
        }
    }

    override suspend fun updateDocument(document: Document): Result<Unit> {
        return try {
            documentDao.updateDocument(document.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to update document", throwable = t)
        }
    }

    private fun Document.toEntity(): DocumentEntity =
        DocumentEntity(
            documentId = documentId,
            caseId = caseId,
            fileName = fileName,
            filePath = filePath,
            fileType = fileType,
            fileSizeBytes = fileSizeBytes,
            isScanned = isScanned,
            thumbnailPath = thumbnailPath,
            tags = tags,
            createdAt = createdAt
        )

    private fun DocumentEntity.toDomain(): Document =
        Document(
            documentId = documentId,
            caseId = caseId,
            fileName = fileName,
            filePath = filePath,
            fileType = fileType,
            fileSizeBytes = fileSizeBytes,
            isScanned = isScanned,
            thumbnailPath = thumbnailPath,
            tags = tags,
            createdAt = createdAt
        )
}

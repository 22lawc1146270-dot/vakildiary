package com.vakildiary.app.domain.storage

import com.vakildiary.app.core.Result
import java.io.InputStream

interface DocumentStorage {
    suspend fun saveDocument(
        caseId: String?,
        fileName: String,
        mimeType: String,
        inputStreamProvider: () -> InputStream,
        isScanned: Boolean,
        tags: String = ""
    ): Result<StoredDocument>

    suspend fun renameDocument(filePath: String, newFileName: String): Result<String>

    suspend fun moveDocument(filePath: String, newCaseId: String?): Result<String>

    suspend fun deleteFile(filePath: String): Result<Unit>

    suspend fun prepareFileForViewing(filePath: String): Result<java.io.File>
}

data class StoredDocument(
    val fileName: String,
    val filePath: String,
    val fileType: String,
    val fileSizeBytes: Long,
    val isScanned: Boolean,
    val thumbnailPath: String?,
    val tags: String
)

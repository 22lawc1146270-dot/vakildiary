package com.vakildiary.app.domain.model

data class Document(
    val documentId: String,
    val caseId: String?,
    val fileName: String,
    val filePath: String,
    val fileType: String,
    val fileSizeBytes: Long,
    val isScanned: Boolean,
    val thumbnailPath: String?,
    val tags: String,
    val createdAt: Long
)

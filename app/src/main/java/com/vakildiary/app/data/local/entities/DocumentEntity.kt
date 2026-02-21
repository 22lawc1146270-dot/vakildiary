package com.vakildiary.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["caseId"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["caseId"])]
)
data class DocumentEntity(
    @PrimaryKey
    val documentId: String, // UUID
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

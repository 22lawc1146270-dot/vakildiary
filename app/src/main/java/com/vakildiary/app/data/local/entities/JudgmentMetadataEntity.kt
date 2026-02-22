package com.vakildiary.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "judgment_metadata")
data class JudgmentMetadataEntity(
    @PrimaryKey
    val judgmentId: String,
    val title: String,
    val citation: String?,
    val bench: String?,
    val year: Int,
    val fileUrl: String,
    val dateOfJudgment: Long?,
    val syncedAt: Long
)

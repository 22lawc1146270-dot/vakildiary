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
    val coram: String?,
    val caseNumber: String?,
    val petitioner: String?,
    val respondent: String?,
    val year: Int,
    val archiveName: String,
    val fileName: String,
    val dateOfJudgment: Long?,
    val searchText: String,
    val syncedAt: Long
)

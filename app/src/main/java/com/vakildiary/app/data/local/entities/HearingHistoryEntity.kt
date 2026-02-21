package com.vakildiary.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hearing_history",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["caseId"],
            childColumns = ["case_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["case_id"])]
)
data class HearingHistoryEntity(
    @PrimaryKey
    val hearingId: String, // UUID
    @ColumnInfo(name = "case_id")
    val caseId: String,
    val hearingDate: Long, // Epoch
    val purpose: String,
    val outcome: String?,
    val orderDetails: String?,
    val nextDateGiven: Long?, // Epoch
    val adjournmentReason: String?,
    val voiceNotePath: String?, // Added from PRD Section 8B.5
    val createdAt: Long
)

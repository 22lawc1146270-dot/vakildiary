package com.vakildiary.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meetings",
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
data class MeetingEntity(
    @PrimaryKey
    val meetingId: String, // UUID
    val caseId: String,
    val clientName: String,
    val meetingDate: Long, // Epoch
    val location: String,
    val agenda: String,
    val notes: String,
    val reminderMinutesBefore: Int,
    val createdAt: Long
)

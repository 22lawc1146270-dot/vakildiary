package com.vakildiary.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vakildiary.app.domain.model.TaskType

@Entity(
    tableName = "tasks",
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
data class TaskEntity(
    @PrimaryKey
    val taskId: String, // UUID
    val caseId: String,
    val title: String,
    val taskType: TaskType,
    val deadline: Long, // Epoch
    val reminderMinutesBefore: Int,
    val isCompleted: Boolean,
    val completedAt: Long?, // Epoch
    val notes: String?,
    val createdAt: Long
)

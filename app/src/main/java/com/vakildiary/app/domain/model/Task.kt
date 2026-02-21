package com.vakildiary.app.domain.model

data class Task(
    val taskId: String,
    val caseId: String,
    val title: String,
    val taskType: TaskType,
    val deadline: Long,
    val reminderMinutesBefore: Int,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val notes: String?,
    val createdAt: Long
)

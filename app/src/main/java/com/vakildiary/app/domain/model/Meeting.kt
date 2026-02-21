package com.vakildiary.app.domain.model

data class Meeting(
    val meetingId: String,
    val caseId: String,
    val clientName: String,
    val meetingDate: Long,
    val location: String,
    val agenda: String,
    val notes: String,
    val reminderMinutesBefore: Int,
    val createdAt: Long
)

package com.vakildiary.app.domain.model

data class HearingHistory(
    val hearingId: String,
    val caseId: String,
    val hearingDate: Long,
    val purpose: String,
    val outcome: String?,
    val orderDetails: String?,
    val nextDateGiven: Long?,
    val adjournmentReason: String?,
    val voiceNotePath: String?,
    val createdAt: Long
)

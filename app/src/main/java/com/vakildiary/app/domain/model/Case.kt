package com.vakildiary.app.domain.model

data class Case(
    val caseId: String,
    val caseName: String,
    val caseNumber: String,
    val courtName: String,
    val courtType: CourtType,
    val clientName: String,
    val clientPhone: String?,
    val clientEmail: String?,
    val oppositeParty: String?,
    val caseType: CaseType,
    val caseStage: CaseStage,
    val customStage: String?,
    val assignedJudge: String?,
    val firNumber: String?,
    val actsAndSections: String?,
    val nextHearingDate: Long?,
    val totalAgreedFees: Double?,
    val isECourtTracked: Boolean,
    val eCourtCaseId: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean
)

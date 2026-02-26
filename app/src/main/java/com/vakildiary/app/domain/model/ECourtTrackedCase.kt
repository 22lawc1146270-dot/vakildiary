package com.vakildiary.app.domain.model

data class ECourtTrackedCase(
    val trackId: String,
    val caseTitle: String,
    val caseNumber: String,
    val year: String,
    val courtName: String,
    val courtType: CourtType?,
    val parties: String,
    val stage: String?,
    val nextHearingDate: String?,
    val caseDetails: List<String>,
    val caseStatus: List<String>,
    val petitionerAdvocate: List<String>,
    val respondentAdvocate: List<String>,
    val acts: List<String>,
    val caseHistory: List<String>,
    val transferDetails: List<String>,
    val createdAt: Long
)

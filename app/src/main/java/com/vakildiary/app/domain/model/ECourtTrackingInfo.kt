package com.vakildiary.app.domain.model

data class ECourtTrackingInfo(
    val stateCode: String,
    val districtCode: String,
    val courtCode: String,
    val caseTypeCode: String,
    val caseNumber: String,
    val year: String,
    val courtName: String,
    val courtType: CourtType?,
    val lastStage: String? = null,
    val lastNextDate: String? = null
)

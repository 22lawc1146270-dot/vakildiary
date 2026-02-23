package com.vakildiary.app.domain.model

data class ECourtRecentEntries(
    val stateCodes: List<String>,
    val districtCodes: List<String>,
    val courtCodes: List<String>,
    val caseTypeCodes: List<String>
)

package com.vakildiary.app.presentation.model

import com.vakildiary.app.domain.model.CourtType

data class ECourtSearchForm(
    val courtType: CourtType? = null,
    val courtName: String = "",
    val stateCode: String = "",
    val districtCode: String = "",
    val courtCode: String = "",
    val establishmentCode: String = "",
    val caseType: String = "",
    val caseNumber: String = "",
    val year: String = "",
    val captcha: String = "",
    val csrfMagic: String = ""
)

data class ECourtCaseItem(
    val caseNumber: String,
    val caseTitle: String,
    val parties: String,
    val nextHearingDate: String,
    val stage: String,
    val courtName: String,
    val courtType: CourtType?,
    val clientName: String
)

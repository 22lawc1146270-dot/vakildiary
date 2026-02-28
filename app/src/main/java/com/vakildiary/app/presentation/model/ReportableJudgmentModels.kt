package com.vakildiary.app.presentation.model

data class ReportableCaseTypeOption(
    val value: String,
    val label: String
)

data class ReportableFormData(
    val sessionId: String,
    val caseTypes: List<ReportableCaseTypeOption>,
    val requiresCaseYear: Boolean,
    val requiresDiaryYear: Boolean,
    val captchaImage: ByteArray,
    val suggestedCaseTypeValue: String?
)

data class ReportableFormInput(
    val sessionId: String,
    val caseTypeValue: String,
    val caseNumber: String,
    val caseYear: String?,
    val diaryYear: String?,
    val captcha: String
)

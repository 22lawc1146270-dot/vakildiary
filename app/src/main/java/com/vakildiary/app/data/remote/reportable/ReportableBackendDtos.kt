package com.vakildiary.app.data.remote.reportable

data class ReportableCaseTypeDto(
    val value: String,
    val label: String
)

data class ReportableFormResponseDto(
    val sessionId: String,
    val caseTypes: List<ReportableCaseTypeDto>,
    val requiresCaseYear: Boolean,
    val requiresDiaryYear: Boolean,
    val captchaImageBase64: String,
    val suggestedCaseTypeValue: String?
)

data class ReportableSubmitRequestDto(
    val sessionId: String,
    val caseTypeValue: String,
    val caseNumber: String,
    val caseYear: String?,
    val diaryYear: String?,
    val captcha: String
)

data class ReportableSubmitResponseDto(
    val downloadUrl: String,
    val fileName: String?
)

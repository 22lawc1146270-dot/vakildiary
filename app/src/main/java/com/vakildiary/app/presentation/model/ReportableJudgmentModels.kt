package com.vakildiary.app.presentation.model

data class ReportableCaseTypeOption(
    val value: String,
    val label: String
)

data class ReportableFormFields(
    val caseType: String,
    val caseNumber: String,
    val caseYear: String?,
    val diaryYear: String?,
    val captcha: String
)

data class ReportableFormData(
    val actionUrl: String,
    val caseTypes: List<ReportableCaseTypeOption>,
    val fields: ReportableFormFields,
    val hiddenFields: Map<String, String>,
    val captchaImage: ByteArray,
    val suggestedCaseTypeValue: String?
)

data class ReportableFormInput(
    val caseTypeValue: String,
    val caseNumber: String,
    val caseYear: String?,
    val diaryYear: String?,
    val captcha: String
)

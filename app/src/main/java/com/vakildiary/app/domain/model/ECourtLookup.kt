package com.vakildiary.app.domain.model

data class ECourtOption(
    val code: String,
    val label: String
)

data class ECourtComplexOption(
    val complexCode: String,
    val courtCodes: String,
    val requiresEstablishment: Boolean,
    val label: String
)

data class ECourtSession(
    val token: String,
    val states: List<ECourtOption>
)

data class ECourtTokenResult<T>(
    val token: String,
    val data: T
)

data class ECourtCaptcha(
    val token: String,
    val imageUrl: String,
    val imageBytes: ByteArray
)

data class ECourtSearchResult(
    val token: String,
    val caseHtml: String,
    val captchaImageUrl: String?,
    val captchaImageBytes: ByteArray?
)

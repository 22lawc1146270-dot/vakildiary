package com.vakildiary.app.domain.model

fun CaseStage.displayLabel(customStage: String? = null): String {
    return when (this) {
        CaseStage.CUSTOM -> customStage?.takeIf { it.isNotBlank() } ?: "Custom"
        CaseStage.UNKNOWN -> "Not set"
        else -> name.replace("_", " ")
    }
}

fun CourtType.displayLabel(): String {
    return if (this == CourtType.UNKNOWN) "Not set" else name.replace("_", " ")
}

fun CaseType.displayLabel(): String {
    return if (this == CaseType.UNKNOWN) "Not set" else name.replace("_", " ")
}

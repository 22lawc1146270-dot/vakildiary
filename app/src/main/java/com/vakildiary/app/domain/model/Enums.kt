package com.vakildiary.app.domain.model

enum class CourtType {
    DISTRICT, HIGH, SUPREME, TRIBUNAL, UNKNOWN
}

enum class CaseType {
    CIVIL, CRIMINAL, WRIT, APPEAL, REVISION, OTHER, UNKNOWN
}

enum class CaseStage {
    FILING, HEARING, ARGUMENTS, JUDGMENT, DISPOSED, CUSTOM, UNKNOWN
}

enum class TaskType {
    FILE_PETITION,
    COLLECT_PAPERS,
    VIEW_ORDERSHEET,
    PHOTOCOPY,
    PAY_COURT_FEES,
    PREPARE_ARGUMENTS,
    MEETING,
    CUSTOM
}

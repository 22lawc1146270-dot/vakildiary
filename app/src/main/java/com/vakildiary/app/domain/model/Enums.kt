package com.vakildiary.app.domain.model

enum class CourtType {
    DISTRICT, HIGH, SUPREME, TRIBUNAL
}

enum class CaseType {
    CIVIL, CRIMINAL, WRIT, APPEAL, REVISION, OTHER
}

enum class CaseStage {
    FILING, HEARING, ARGUMENTS, JUDGMENT, DISPOSED
}

enum class TaskType {
    FILE_PETITION, COLLECT_PAPERS, VIEW_ORDERSHEET, PHOTOCOPY, MEETING, CUSTOM
}

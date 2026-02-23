package com.vakildiary.app.domain.model

enum class BackupSchedule {
    MANUAL,
    DAILY,
    WEEKLY
}

enum class BackupStatus {
    NONE,
    SUCCESS,
    ERROR
}

data class BackupLogEntry(
    val timestamp: Long,
    val status: BackupStatus,
    val message: String
)

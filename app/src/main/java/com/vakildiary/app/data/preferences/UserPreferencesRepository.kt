package com.vakildiary.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.vakildiary.app.domain.model.BackupLogEntry
import com.vakildiary.app.domain.model.BackupSchedule
import com.vakildiary.app.domain.model.BackupStatus
import com.vakildiary.app.presentation.theme.ThemeMode
import com.vakildiary.app.presentation.theme.LanguageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val userEmail: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL]
    }

    val isSignInSkipped: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SIGN_IN_SKIPPED] ?: false
    }

    val isRestorePromptShown: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_RESTORE_PROMPT_SHOWN] ?: false
    }

    val isNotificationPromptShown: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATION_PROMPT_SHOWN] ?: false
    }

    val backupSchedule: Flow<BackupSchedule> = dataStore.data.map { preferences ->
        val value = preferences[KEY_BACKUP_SCHEDULE] ?: BackupSchedule.MANUAL.name
        BackupSchedule.valueOf(value)
    }

    val lastBackupTime: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_BACKUP_TIME]
    }

    val lastBackupSizeBytes: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_BACKUP_SIZE_BYTES]
    }

    val lastBackupStatus: Flow<BackupStatus> = dataStore.data.map { preferences ->
        val value = preferences[KEY_LAST_BACKUP_STATUS] ?: BackupStatus.NONE.name
        BackupStatus.valueOf(value)
    }

    val lastBackupMessage: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_BACKUP_MESSAGE]
    }

    val backupLog: Flow<List<BackupLogEntry>> = dataStore.data.map { preferences ->
        parseBackupLog(preferences[KEY_BACKUP_LOG])
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val value = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(value)
    }

    val languageMode: Flow<LanguageMode> = dataStore.data.map { preferences ->
        val value = preferences[KEY_LANGUAGE_MODE] ?: LanguageMode.SYSTEM.name
        LanguageMode.valueOf(value)
    }

    val isAppLockEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_APP_LOCK_ENABLED] ?: false
    }

    val advocateName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_ADVOCATE_NAME]
    }

    suspend fun setUserEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_SIGN_IN_SKIPPED] = false
        }
    }

    suspend fun setSignInSkipped(isSkipped: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SIGN_IN_SKIPPED] = isSkipped
            if (isSkipped) {
                preferences.remove(KEY_USER_EMAIL)
            }
        }
    }

    suspend fun clearUser() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_USER_EMAIL)
            preferences[KEY_SIGN_IN_SKIPPED] = false
        }
    }

    suspend fun setRestorePromptShown(isShown: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_RESTORE_PROMPT_SHOWN] = isShown
        }
    }

    suspend fun setNotificationPromptShown(isShown: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_PROMPT_SHOWN] = isShown
        }
    }

    suspend fun setBackupSchedule(schedule: BackupSchedule) {
        dataStore.edit { preferences ->
            preferences[KEY_BACKUP_SCHEDULE] = schedule.name
        }
    }

    suspend fun recordBackupResult(
        status: BackupStatus,
        message: String?,
        sizeBytes: Long?,
        timestampMillis: Long
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP_TIME] = timestampMillis
            if (sizeBytes != null) {
                preferences[KEY_LAST_BACKUP_SIZE_BYTES] = sizeBytes
            } else {
                preferences.remove(KEY_LAST_BACKUP_SIZE_BYTES)
            }
            preferences[KEY_LAST_BACKUP_STATUS] = status.name
            if (message.isNullOrBlank()) {
                preferences.remove(KEY_LAST_BACKUP_MESSAGE)
            } else {
                preferences[KEY_LAST_BACKUP_MESSAGE] = message
            }

            val entry = buildLogEntry(timestampMillis, status, message)
            val existing = preferences[KEY_BACKUP_LOG].orEmpty()
            val updated = listOf(entry) + existing.lines().filter { it.isNotBlank() }
            preferences[KEY_BACKUP_LOG] = updated.take(LOG_LIMIT).joinToString("\n")
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE_MODE] = mode.name
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAdvocateName(name: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ADVOCATE_NAME] = name
        }
    }

    private fun parseBackupLog(raw: String?): List<BackupLogEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split(LOG_DELIMITER, limit = 3)
            if (parts.size < 3) return@mapNotNull null
            val timestamp = parts[0].toLongOrNull() ?: return@mapNotNull null
            val status = runCatching { BackupStatus.valueOf(parts[1]) }.getOrNull() ?: BackupStatus.NONE
            BackupLogEntry(timestamp = timestamp, status = status, message = parts[2])
        }
    }

    private fun buildLogEntry(timestampMillis: Long, status: BackupStatus, message: String?): String {
        val safeMessage = message?.replace("\n", " ").orEmpty()
        return listOf(timestampMillis.toString(), status.name, safeMessage).joinToString(LOG_DELIMITER)
    }

    companion object {
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_SIGN_IN_SKIPPED = booleanPreferencesKey("sign_in_skipped")
        private val KEY_RESTORE_PROMPT_SHOWN = booleanPreferencesKey("restore_prompt_shown")
        private val KEY_NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_LANGUAGE_MODE = stringPreferencesKey("language_mode")
        private val KEY_ADVOCATE_NAME = stringPreferencesKey("advocate_name")
        private val KEY_BACKUP_SCHEDULE = stringPreferencesKey("backup_schedule")
        private val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        private val KEY_LAST_BACKUP_SIZE_BYTES = longPreferencesKey("last_backup_size_bytes")
        private val KEY_LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
        private val KEY_LAST_BACKUP_MESSAGE = stringPreferencesKey("last_backup_message")
        private val KEY_BACKUP_LOG = stringPreferencesKey("backup_log")
        private const val LOG_DELIMITER = "|"
        private const val LOG_LIMIT = 20
    }
}

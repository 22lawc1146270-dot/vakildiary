package com.vakildiary.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    companion object {
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_SIGN_IN_SKIPPED = booleanPreferencesKey("sign_in_skipped")
        private val KEY_RESTORE_PROMPT_SHOWN = booleanPreferencesKey("restore_prompt_shown")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_LANGUAGE_MODE = stringPreferencesKey("language_mode")
    }
}

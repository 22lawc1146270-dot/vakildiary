package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.data.preferences.UserPreferencesRepository
import com.vakildiary.app.presentation.theme.ThemeMode
import com.vakildiary.app.presentation.theme.LanguageMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val languageMode: StateFlow<LanguageMode> = userPreferencesRepository.languageMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanguageMode.SYSTEM)

    val advocateName: StateFlow<String?> = userPreferencesRepository.advocateName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isNotificationPromptShown: StateFlow<Boolean> = userPreferencesRepository.isNotificationPromptShown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguageMode(mode)
        }
    }

    fun setAdvocateName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAdvocateName(name)
        }
    }

    fun setNotificationPromptShown(isShown: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationPromptShown(isShown)
        }
    }
}

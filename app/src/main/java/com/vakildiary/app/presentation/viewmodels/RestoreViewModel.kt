package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.backup.RestoreManager
import com.vakildiary.app.data.preferences.UserPreferencesRepository
import com.vakildiary.app.presentation.viewmodels.state.RestoreUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RestoreViewModel @Inject constructor(
    private val restoreManager: RestoreManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()

    fun checkForRestore() {
        viewModelScope.launch {
            _uiState.value = RestoreUiState.Checking
            val promptShown = userPreferencesRepository.isRestorePromptShown.first()
            val hasLocalData = restoreManager.hasLocalData()

            if (promptShown || hasLocalData) {
                _uiState.value = RestoreUiState.NotAvailable
                return@launch
            }

            when (val remote = restoreManager.hasRemoteBackup()) {
                is Result.Success -> {
                    _uiState.value = if (remote.data) RestoreUiState.Available else RestoreUiState.NotAvailable
                }
                is Result.Error -> {
                    _uiState.value = RestoreUiState.Error(remote.message)
                }
            }
        }
    }

    fun restoreNow() {
        viewModelScope.launch {
            _uiState.value = RestoreUiState.Restoring
            _uiState.value = when (val result = restoreManager.restoreLatestBackup()) {
                is Result.Success -> RestoreUiState.Restored
                is Result.Error -> RestoreUiState.Error(result.message)
            }
            userPreferencesRepository.setRestorePromptShown(true)
        }
    }

    fun skipRestore() {
        viewModelScope.launch {
            userPreferencesRepository.setRestorePromptShown(true)
            _uiState.value = RestoreUiState.NotAvailable
        }
    }
}

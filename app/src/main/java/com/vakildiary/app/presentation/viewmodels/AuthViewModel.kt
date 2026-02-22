package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.data.preferences.UserPreferencesRepository
import com.vakildiary.app.presentation.viewmodels.state.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userEmail: StateFlow<String?> = userPreferencesRepository.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isSignInSkipped: StateFlow<Boolean> = userPreferencesRepository.isSignInSkipped
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow<AuthUiState>(AuthUiState.Success(""))
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onSignInSuccess(email: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserEmail(email)
            _uiState.value = AuthUiState.Success(email)
        }
    }

    fun onSignInError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun onLoading() {
        _uiState.value = AuthUiState.Loading
    }

    fun skipSignIn() {
        viewModelScope.launch {
            userPreferencesRepository.setSignInSkipped(true)
            _uiState.value = AuthUiState.Success("")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userPreferencesRepository.clearUser()
            _uiState.value = AuthUiState.Success("")
        }
    }
}

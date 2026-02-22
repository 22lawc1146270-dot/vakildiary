package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.domain.usecase.cases.GetAllCasesUseCase
import com.vakildiary.app.domain.usecase.judgment.SearchJudgmentsUseCase
import com.vakildiary.app.domain.usecase.judgment.DownloadJudgmentUseCase
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JudgmentSearchViewModel @Inject constructor(
    private val searchJudgmentsUseCase: SearchJudgmentsUseCase,
    private val downloadJudgmentUseCase: DownloadJudgmentUseCase,
    getAllCasesUseCase: GetAllCasesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<JudgmentSearchUiState>(JudgmentSearchUiState.Success(emptyList()))
    val uiState: StateFlow<JudgmentSearchUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<JudgmentDownloadUiState>(JudgmentDownloadUiState.Success(""))
    val downloadState: StateFlow<JudgmentDownloadUiState> = _downloadState.asStateFlow()

    val casesState: StateFlow<CasePickerUiState> = getAllCasesUseCase()
        .map { result ->
            when (result) {
                is Result.Success -> CasePickerUiState.Success(result.data)
                is Result.Error -> CasePickerUiState.Error(result.message)
            }
        }
        .catch { emit(CasePickerUiState.Error("Failed to load cases")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CasePickerUiState.Loading)

    fun search(query: String, year: String) {
        if (year.isBlank()) {
            _uiState.value = JudgmentSearchUiState.Error("Please enter a year")
            return
        }
        viewModelScope.launch {
            _uiState.value = JudgmentSearchUiState.Loading
            _uiState.value = when (val result = searchJudgmentsUseCase(query.trim(), year.trim())) {
                is Result.Success -> JudgmentSearchUiState.Success(result.data)
                is Result.Error -> JudgmentSearchUiState.Error(result.message)
            }
        }
    }

    fun download(item: JudgmentSearchResult, caseId: String?) {
        viewModelScope.launch {
            _downloadState.value = JudgmentDownloadUiState.Loading
            _downloadState.value = when (val result = downloadJudgmentUseCase(item, caseId)) {
                is Result.Success -> JudgmentDownloadUiState.Success("Judgment downloaded")
                is Result.Error -> JudgmentDownloadUiState.Error(result.message)
            }
        }
    }
}

sealed interface JudgmentSearchUiState {
    object Loading : JudgmentSearchUiState
    data class Success(val results: List<JudgmentSearchResult>) : JudgmentSearchUiState
    data class Error(val message: String) : JudgmentSearchUiState
}

sealed interface JudgmentDownloadUiState {
    object Loading : JudgmentDownloadUiState
    data class Success(val message: String) : JudgmentDownloadUiState
    data class Error(val message: String) : JudgmentDownloadUiState
}

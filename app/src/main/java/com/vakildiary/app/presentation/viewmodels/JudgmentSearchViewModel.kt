package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.domain.usecase.documents.PrepareDocumentForViewingUseCase
import com.vakildiary.app.domain.usecase.judgment.SearchJudgmentsUseCase
import com.vakildiary.app.domain.usecase.judgment.DownloadJudgmentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class JudgmentSearchViewModel @Inject constructor(
    private val searchJudgmentsUseCase: SearchJudgmentsUseCase,
    private val downloadJudgmentUseCase: DownloadJudgmentUseCase,
    private val prepareDocumentForViewingUseCase: PrepareDocumentForViewingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<JudgmentSearchUiState>(JudgmentSearchUiState.Success(emptyList()))
    val uiState: StateFlow<JudgmentSearchUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<JudgmentDownloadUiState>(JudgmentDownloadUiState.Success(""))
    val downloadState: StateFlow<JudgmentDownloadUiState> = _downloadState.asStateFlow()

    private val _syncState = MutableStateFlow<JudgmentSyncState>(JudgmentSyncState.Idle)
    val syncState: StateFlow<JudgmentSyncState> = _syncState.asStateFlow()

    private val _fileEvents = MutableSharedFlow<Result<File>>(extraBufferCapacity = 1)
    val fileEvents = _fileEvents

    fun search(query: String, year: String) {
        if (year.isBlank()) {
            _uiState.value = JudgmentSearchUiState.Error("Please enter a year")
            return
        }
        _downloadState.value = JudgmentDownloadUiState.Success("")
        viewModelScope.launch {
            _syncState.value = JudgmentSyncState.Syncing("Preparing judgments for $year...")
            _uiState.value = JudgmentSearchUiState.Loading
            _uiState.value = try {
                when (val result = searchJudgmentsUseCase(query.trim(), year.trim())) {
                    is Result.Success -> JudgmentSearchUiState.Success(result.data)
                    is Result.Error -> JudgmentSearchUiState.Error(result.message)
                }
            } finally {
                _syncState.value = JudgmentSyncState.Idle
            }
        }
    }

    fun download(item: JudgmentSearchResult, year: String) {
        viewModelScope.launch {
            _downloadState.value = JudgmentDownloadUiState.Loading
            _downloadState.value = when (val result = downloadJudgmentUseCase(item, year)) {
                is Result.Success -> JudgmentDownloadUiState.Success("Judgment downloaded", result.data)
                is Result.Error -> JudgmentDownloadUiState.Error(result.message)
            }
        }
    }

    fun prepareFileForViewing(document: Document) {
        viewModelScope.launch {
            _fileEvents.emit(prepareDocumentForViewingUseCase(document.filePath))
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
    data class Success(val message: String, val document: Document? = null) : JudgmentDownloadUiState
    data class Error(val message: String) : JudgmentDownloadUiState
}

sealed interface JudgmentSyncState {
    object Idle : JudgmentSyncState
    data class Syncing(val message: String) : JudgmentSyncState
}

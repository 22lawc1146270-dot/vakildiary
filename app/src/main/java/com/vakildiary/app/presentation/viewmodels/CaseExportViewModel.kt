package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.domain.usecase.pdf.ExportCaseHistoryUseCase
import com.vakildiary.app.domain.usecase.pdf.ExportCaseSummaryUseCase
import com.vakildiary.app.presentation.viewmodels.state.CaseExportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaseExportViewModel @Inject constructor(
    private val exportCaseSummaryUseCase: ExportCaseSummaryUseCase,
    private val exportCaseHistoryUseCase: ExportCaseHistoryUseCase
) : ViewModel() {

    private val _summaryState = MutableStateFlow<CaseExportUiState>(CaseExportUiState.Success(null))
    val summaryState: StateFlow<CaseExportUiState> = _summaryState.asStateFlow()

    private val _historyState = MutableStateFlow<CaseExportUiState>(CaseExportUiState.Success(null))
    val historyState: StateFlow<CaseExportUiState> = _historyState.asStateFlow()

    fun exportSummary(case: Case) {
        viewModelScope.launch {
            _summaryState.value = CaseExportUiState.Loading
            _summaryState.value = when (val result = exportCaseSummaryUseCase(case)) {
                is Result.Success -> CaseExportUiState.Success(result.data)
                is Result.Error -> CaseExportUiState.Error(result.message)
            }
        }
    }

    fun exportHistory(case: Case, hearings: List<HearingHistory>) {
        viewModelScope.launch {
            _historyState.value = CaseExportUiState.Loading
            _historyState.value = when (val result = exportCaseHistoryUseCase(case, hearings)) {
                is Result.Success -> CaseExportUiState.Success(result.data)
                is Result.Error -> CaseExportUiState.Error(result.message)
            }
        }
    }

    fun resetSummaryState() {
        _summaryState.value = CaseExportUiState.Success(null)
    }

    fun resetHistoryState() {
        _historyState.value = CaseExportUiState.Success(null)
    }
}

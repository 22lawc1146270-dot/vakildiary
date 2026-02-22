package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.Payment
import com.vakildiary.app.domain.usecase.pdf.ExportFeeLedgerUseCase
import com.vakildiary.app.presentation.viewmodels.state.FeeExportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeesViewModel @Inject constructor(
    private val exportFeeLedgerUseCase: ExportFeeLedgerUseCase
) : ViewModel() {

    private val _exportState = MutableStateFlow<FeeExportUiState>(FeeExportUiState.Success(null))
    val exportState: StateFlow<FeeExportUiState> = _exportState.asStateFlow()

    fun exportFeeLedger(case: Case, payments: List<Payment>) {
        viewModelScope.launch {
            _exportState.value = FeeExportUiState.Loading
            _exportState.value = when (val result = exportFeeLedgerUseCase(case, payments)) {
                is Result.Success -> FeeExportUiState.Success(result.data)
                is Result.Error -> FeeExportUiState.Error(result.message)
            }
        }
    }

    fun resetExportState() {
        _exportState.value = FeeExportUiState.Success(null)
    }
}

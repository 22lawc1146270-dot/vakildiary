package com.vakildiary.app.presentation.viewmodels.state

import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.domain.model.Payment
import com.vakildiary.app.domain.model.Task

sealed interface CaseDetailUiState {
    object Loading : CaseDetailUiState
    data class Success(
        val case: Case,
        val hearings: List<HearingHistory>,
        val tasks: List<Task>,
        val payments: List<Payment>
    ) : CaseDetailUiState
    data class Error(val message: String) : CaseDetailUiState
}

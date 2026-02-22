package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.usecase.cases.GetCaseByIdUseCase
import com.vakildiary.app.domain.usecase.cases.UpdateCaseUseCase
import com.vakildiary.app.domain.usecase.cases.ArchiveCaseUseCase
import com.vakildiary.app.domain.usecase.hearing.GetHearingHistoryUseCase
import com.vakildiary.app.domain.usecase.payment.GetPaymentsByCaseUseCase
import com.vakildiary.app.domain.usecase.task.GetPendingTasksUseCase
import com.vakildiary.app.presentation.viewmodels.state.CaseDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

@HiltViewModel
class CaseDetailViewModel @Inject constructor(
    private val getCaseByIdUseCase: GetCaseByIdUseCase,
    private val updateCaseUseCase: UpdateCaseUseCase,
    private val archiveCaseUseCase: ArchiveCaseUseCase,
    private val getHearingHistoryUseCase: GetHearingHistoryUseCase,
    private val getPendingTasksUseCase: GetPendingTasksUseCase,
    private val getPaymentsByCaseUseCase: GetPaymentsByCaseUseCase
) : ViewModel() {

    fun uiState(caseId: String): StateFlow<CaseDetailUiState> {
        return combine(
            getCaseByIdUseCase(caseId),
            getHearingHistoryUseCase(caseId),
            getPendingTasksUseCase(caseId),
            getPaymentsByCaseUseCase(caseId)
        ) { caseResult, hearingResult, taskResult, paymentResult ->
            val case = (caseResult as? Result.Success)?.data
            val hearings = (hearingResult as? Result.Success)?.data.orEmpty()
            val tasks = (taskResult as? Result.Success)?.data.orEmpty()
            val payments = (paymentResult as? Result.Success)?.data.orEmpty()

            if (case == null) {
                CaseDetailUiState.Error("Case not found")
            } else {
                CaseDetailUiState.Success(case, hearings, tasks, payments)
            }
        }
            .catch { emit(CaseDetailUiState.Error("Failed to load case detail")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaseDetailUiState.Loading)
    }

    suspend fun updateCase(case: com.vakildiary.app.domain.model.Case): Result<Unit> {
        return updateCaseUseCase(case)
    }

    suspend fun archiveCase(caseId: String): Result<Unit> {
        return archiveCaseUseCase(caseId)
    }
}

package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.domain.usecase.cases.GetAllCasesUseCase
import com.vakildiary.app.domain.usecase.cases.GetCaseByIdUseCase
import com.vakildiary.app.domain.usecase.cases.UpdateCaseUseCase
import com.vakildiary.app.domain.usecase.hearing.AddHearingUseCase
import com.vakildiary.app.presentation.viewmodels.state.AddHearingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddHearingViewModel @Inject constructor(
    private val getAllCasesUseCase: GetAllCasesUseCase,
    private val getCaseByIdUseCase: GetCaseByIdUseCase,
    private val updateCaseUseCase: UpdateCaseUseCase,
    private val addHearingUseCase: AddHearingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddHearingUiState>(AddHearingUiState.Loading)
    val uiState: StateFlow<AddHearingUiState> = _uiState.asStateFlow()

    fun loadCases() {
        viewModelScope.launch {
            _uiState.value = AddHearingUiState.Loading
            getAllCasesUseCase().collect { result ->
                _uiState.value = when (result) {
                    is Result.Success -> AddHearingUiState.Success(result.data)
                    is Result.Error -> AddHearingUiState.Error(result.message)
                }
            }
        }
    }

    fun saveHearing(
        caseId: String,
        hearingDateMillis: Long,
        purpose: String,
        reminderMinutesBefore: Int
    ) {
        viewModelScope.launch {
            val caseResult = getCaseByIdUseCase(caseId).first()
            val case = (caseResult as? Result.Success)?.data ?: return@launch

            val hearing = HearingHistory(
                hearingId = UUID.randomUUID().toString(),
                caseId = caseId,
                hearingDate = hearingDateMillis,
                purpose = purpose,
                outcome = null,
                orderDetails = null,
                nextDateGiven = null,
                adjournmentReason = null,
                voiceNotePath = null,
                createdAt = System.currentTimeMillis()
            )

            addHearingUseCase(hearing)
            updateCaseUseCase(case.copy(nextHearingDate = hearingDateMillis))
        }
    }
}

package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.domain.usecase.cases.GetCaseByIdUseCase
import com.vakildiary.app.domain.usecase.cases.UpdateCaseUseCase
import com.vakildiary.app.presentation.viewmodels.state.AddCaseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditCaseViewModel @Inject constructor(
    private val getCaseByIdUseCase: GetCaseByIdUseCase,
    private val updateCaseUseCase: UpdateCaseUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(AddCaseFormState())
    val formState: StateFlow<AddCaseFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AddCaseUiState>(AddCaseUiState.Loading)
    val uiState: StateFlow<AddCaseUiState> = _uiState.asStateFlow()

    private var currentCase: Case? = null

    fun load(caseId: String) {
        viewModelScope.launch {
            _uiState.value = AddCaseUiState.Loading
            when (val result = getCaseByIdUseCase(caseId).first()) {
                is Result.Success -> {
                    val case = result.data ?: run {
                        _uiState.value = AddCaseUiState.Error("Case not found")
                        return@launch
                    }
                    currentCase = case
                    _formState.value = AddCaseFormState(
                        caseName = case.caseName,
                        caseNumber = case.caseNumber,
                        courtType = case.courtType,
                        courtName = case.courtName,
                        clientName = case.clientName,
                        caseType = case.caseType,
                        caseStage = case.caseStage,
                        customStage = case.customStage.orEmpty(),
                        oppositeParty = case.oppositeParty.orEmpty(),
                        assignedJudge = case.assignedJudge.orEmpty(),
                        firNumber = case.firNumber.orEmpty(),
                        actsAndSections = case.actsAndSections.orEmpty(),
                        clientPhone = case.clientPhone.orEmpty(),
                        clientEmail = case.clientEmail.orEmpty(),
                        totalAgreedFees = case.totalAgreedFees?.toString().orEmpty(),
                        notes = case.notes.orEmpty()
                    )
                    _uiState.value = AddCaseUiState.Success(isSaved = false)
                }
                is Result.Error -> _uiState.value = AddCaseUiState.Error(result.message)
            }
        }
    }

    fun onCaseNameChanged(value: String) = updateForm { it.copy(caseName = value) }
    fun onCaseNumberChanged(value: String) = updateForm { it.copy(caseNumber = value) }
    fun onCourtTypeChanged(value: CourtType) = updateForm { it.copy(courtType = value) }
    fun onCourtNameChanged(value: String) = updateForm { it.copy(courtName = value) }
    fun onClientNameChanged(value: String) = updateForm { it.copy(clientName = value) }
    fun onCaseTypeChanged(value: CaseType) = updateForm { it.copy(caseType = value) }
    fun onCaseStageChanged(value: CaseStage) = updateForm {
        it.copy(
            caseStage = value,
            customStage = if (value == CaseStage.CUSTOM) it.customStage else ""
        )
    }
    fun onCustomStageChanged(value: String) = updateForm { it.copy(customStage = value) }
    fun onOppositePartyChanged(value: String) = updateForm { it.copy(oppositeParty = value) }
    fun onJudgeChanged(value: String) = updateForm { it.copy(assignedJudge = value) }
    fun onFirNumberChanged(value: String) = updateForm { it.copy(firNumber = value) }
    fun onActsChanged(value: String) = updateForm { it.copy(actsAndSections = value) }
    fun onPhoneChanged(value: String) = updateForm { it.copy(clientPhone = value) }
    fun onEmailChanged(value: String) = updateForm { it.copy(clientEmail = value) }
    fun onFeesChanged(value: String) = updateForm { it.copy(totalAgreedFees = value) }
    fun onNotesChanged(value: String) = updateForm { it.copy(notes = value) }

    fun onSave() {
        val state = formState.value
        val base = currentCase ?: return
        val fees = state.totalAgreedFees.trim()
        val parsedFees = if (fees.isBlank()) null else fees.toDoubleOrNull()

        val customStage = state.customStage.trim().ifBlank { null }
        val stage = when (state.caseStage) {
            CaseStage.CUSTOM -> {
                if (customStage == null) {
                    _uiState.value = AddCaseUiState.Error("Please enter a custom stage")
                    return
                }
                CaseStage.CUSTOM
            }
            null -> base.caseStage
            else -> state.caseStage
        }

        val updated = base.copy(
            caseName = state.caseName.trim(),
            caseNumber = state.caseNumber.trim(),
            courtType = state.courtType ?: base.courtType,
            courtName = state.courtName.trim(),
            clientName = state.clientName.trim(),
            caseType = state.caseType ?: base.caseType,
            caseStage = stage,
            customStage = if (stage == CaseStage.CUSTOM) customStage else null,
            oppositeParty = state.oppositeParty.trim().ifBlank { null },
            assignedJudge = state.assignedJudge.trim().ifBlank { null },
            firNumber = state.firNumber.trim().ifBlank { null },
            actsAndSections = state.actsAndSections.trim().ifBlank { null },
            clientPhone = state.clientPhone.trim().ifBlank { null },
            clientEmail = state.clientEmail.trim().ifBlank { null },
            totalAgreedFees = parsedFees,
            notes = state.notes.trim().ifBlank { null },
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.value = AddCaseUiState.Loading
            _uiState.value = when (val result = updateCaseUseCase(updated)) {
                is Result.Success -> AddCaseUiState.Success(isSaved = true)
                is Result.Error -> AddCaseUiState.Error(result.message)
            }
        }
    }

    private fun updateForm(block: (AddCaseFormState) -> AddCaseFormState) {
        _formState.value = block(_formState.value)
    }
}

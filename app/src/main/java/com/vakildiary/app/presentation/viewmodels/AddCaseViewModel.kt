package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.ecourt.ECourtTrackingStore
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.domain.model.ECourtTrackingInfo
import com.vakildiary.app.domain.usecase.cases.AddCaseUseCase
import com.vakildiary.app.domain.usecase.cases.GetCaseByNumberUseCase
import com.vakildiary.app.presentation.viewmodels.state.AddCaseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddCaseViewModel @Inject constructor(
    private val addCaseUseCase: AddCaseUseCase,
    private val getCaseByNumberUseCase: GetCaseByNumberUseCase,
    private val ecourtTrackingStore: ECourtTrackingStore
) : ViewModel() {

    private val _formState = MutableStateFlow(AddCaseFormState())
    val formState: StateFlow<AddCaseFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AddCaseUiState>(AddCaseUiState.Success(isSaved = false))
    val uiState: StateFlow<AddCaseUiState> = _uiState.asStateFlow()
    private val _warning = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val warning = _warning
    private var ecourtTrackingInfo: ECourtTrackingInfo? = null

    fun onCaseNameChanged(value: String) = updateForm { it.copy(caseName = value) }
    fun onCaseNumberChanged(value: String) = updateForm { it.copy(caseNumber = value) }
    fun onCourtTypeChanged(value: CourtType) = updateForm { it.copy(courtType = value) }
    fun onCourtNameChanged(value: String) = updateForm { it.copy(courtName = value) }
    fun onClientNameChanged(value: String) = updateForm { it.copy(clientName = value) }
    fun onCaseTypeChanged(value: CaseType) = updateForm { it.copy(caseType = value) }
    fun onCaseStageChanged(value: CaseStage) = updateForm { it.copy(caseStage = value) }

    fun onOppositePartyChanged(value: String) = updateForm { it.copy(oppositeParty = value) }
    fun onJudgeChanged(value: String) = updateForm { it.copy(assignedJudge = value) }
    fun onFirNumberChanged(value: String) = updateForm { it.copy(firNumber = value) }
    fun onActsChanged(value: String) = updateForm { it.copy(actsAndSections = value) }
    fun onPhoneChanged(value: String) = updateForm { it.copy(clientPhone = value) }
    fun onEmailChanged(value: String) = updateForm { it.copy(clientEmail = value) }
    fun onFeesChanged(value: String) = updateForm { it.copy(totalAgreedFees = value) }
    fun onNotesChanged(value: String) = updateForm { it.copy(notes = value) }

    fun prefill(
        caseName: String?,
        caseNumber: String?,
        courtName: String?,
        clientName: String?,
        courtType: CourtType?,
        caseType: CaseType?,
        caseStage: CaseStage?
    ) {
        _formState.value = _formState.value.copy(
            caseName = caseName?.takeIf { it.isNotBlank() } ?: _formState.value.caseName,
            caseNumber = caseNumber?.takeIf { it.isNotBlank() } ?: _formState.value.caseNumber,
            courtName = courtName?.takeIf { it.isNotBlank() } ?: _formState.value.courtName,
            clientName = clientName?.takeIf { it.isNotBlank() } ?: _formState.value.clientName,
            courtType = courtType ?: _formState.value.courtType,
            caseType = caseType ?: _formState.value.caseType,
            caseStage = caseStage ?: _formState.value.caseStage
        )
    }

    fun setECourtTracking(
        stateCode: String,
        districtCode: String,
        courtCode: String,
        caseTypeCode: String,
        year: String,
        courtName: String,
        courtType: CourtType?,
        caseNumber: String
    ) {
        ecourtTrackingInfo = ECourtTrackingInfo(
            stateCode = stateCode.trim(),
            districtCode = districtCode.trim(),
            courtCode = courtCode.trim(),
            caseTypeCode = caseTypeCode.trim(),
            caseNumber = caseNumber.trim(),
            year = year.trim(),
            courtName = courtName.trim(),
            courtType = courtType
        )
    }

    fun clearECourtTracking() {
        ecourtTrackingInfo = null
    }

    fun onSave() {
        val state = formState.value
        if (!state.isValid()) {
            _uiState.value = AddCaseUiState.Error("Please fill all mandatory fields")
            return
        }

        val fees = state.totalAgreedFees.trim()
        val parsedFees = if (fees.isBlank()) null else fees.toDoubleOrNull()
        if (fees.isNotBlank() && (parsedFees == null || parsedFees <= 0.0)) {
            _uiState.value = AddCaseUiState.Error("Total agreed fees must be a positive number")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddCaseUiState.Loading
            when (val duplicate = getCaseByNumberUseCase(state.caseNumber.trim())) {
                is Result.Success -> if (duplicate.data != null) {
                    _warning.tryEmit("Duplicate case number found")
                }
                is Result.Error -> Unit
            }
            val now = System.currentTimeMillis()
            val case = Case(
                caseId = UUID.randomUUID().toString(),
                caseName = state.caseName.trim(),
                caseNumber = state.caseNumber.trim(),
                courtName = state.courtName.trim(),
                courtType = state.courtType!!,
                clientName = state.clientName.trim(),
                clientPhone = state.clientPhone.trim().ifBlank { null },
                clientEmail = state.clientEmail.trim().ifBlank { null },
                oppositeParty = state.oppositeParty.trim().ifBlank { null },
                caseType = state.caseType!!,
                caseStage = state.caseStage!!,
                assignedJudge = state.assignedJudge.trim().ifBlank { null },
                firNumber = state.firNumber.trim().ifBlank { null },
                actsAndSections = state.actsAndSections.trim().ifBlank { null },
                nextHearingDate = null,
                totalAgreedFees = parsedFees,
                isECourtTracked = ecourtTrackingInfo != null,
                eCourtCaseId = ecourtTrackingInfo?.caseNumber,
                notes = state.notes.trim().ifBlank { null },
                createdAt = now,
                updatedAt = now,
                isArchived = false
            )

            _uiState.value = when (val result = addCaseUseCase(case)) {
                is Result.Success -> {
                    ecourtTrackingInfo?.let { info ->
                        ecourtTrackingStore.save(
                            caseId = case.caseId,
                            info = info.copy(lastStage = case.caseStage.name)
                        )
                    }
                    AddCaseUiState.Success(isSaved = true)
                }
                is Result.Error -> AddCaseUiState.Error(result.message)
            }
        }
    }

    private fun updateForm(block: (AddCaseFormState) -> AddCaseFormState) {
        _formState.value = block(_formState.value)
    }
}

data class AddCaseFormState(
    val caseName: String = "",
    val caseNumber: String = "",
    val courtType: CourtType? = null,
    val courtName: String = "",
    val clientName: String = "",
    val caseType: CaseType? = null,
    val caseStage: CaseStage? = null,
    val oppositeParty: String = "",
    val assignedJudge: String = "",
    val firNumber: String = "",
    val actsAndSections: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val totalAgreedFees: String = "",
    val notes: String = ""
) {
    fun isValid(): Boolean {
        return caseName.isNotBlank() &&
            caseNumber.isNotBlank() &&
            courtType != null &&
            courtName.isNotBlank() &&
            clientName.isNotBlank() &&
            caseType != null &&
            caseStage != null
    }
}

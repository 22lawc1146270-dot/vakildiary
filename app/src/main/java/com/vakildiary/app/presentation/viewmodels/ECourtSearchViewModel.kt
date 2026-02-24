package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.ecourt.ECourtTrackingStore
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.ECourtComplexOption
import com.vakildiary.app.domain.model.ECourtOption
import com.vakildiary.app.domain.model.ECourtTokenResult
import com.vakildiary.app.domain.usecase.cases.GetCaseByNumberUseCase
import com.vakildiary.app.domain.usecase.cases.UpdateCaseUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtCaptchaUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtCaseTypesUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtCourtComplexesUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtDistrictsUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtEstablishmentsUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtSessionUseCase
import com.vakildiary.app.domain.usecase.ecourt.SearchECourtUseCase
import com.vakildiary.app.notifications.ECourtStatusNotifier
import com.vakildiary.app.presentation.model.ECourtCaseItem
import com.vakildiary.app.presentation.model.ECourtParser
import com.vakildiary.app.presentation.model.ECourtSearchForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ECourtSearchViewModel @Inject constructor(
    private val searchECourtUseCase: SearchECourtUseCase,
    private val fetchSessionUseCase: FetchECourtSessionUseCase,
    private val fetchDistrictsUseCase: FetchECourtDistrictsUseCase,
    private val fetchCourtComplexesUseCase: FetchECourtCourtComplexesUseCase,
    private val fetchEstablishmentsUseCase: FetchECourtEstablishmentsUseCase,
    private val fetchCaseTypesUseCase: FetchECourtCaseTypesUseCase,
    private val fetchCaptchaUseCase: FetchECourtCaptchaUseCase,
    private val getCaseByNumberUseCase: GetCaseByNumberUseCase,
    private val updateCaseUseCase: UpdateCaseUseCase,
    private val trackingStore: ECourtTrackingStore,
    private val notifier: ECourtStatusNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow<ECourtSearchUiState>(ECourtSearchUiState.Success(emptyList()))
    val uiState: StateFlow<ECourtSearchUiState> = _uiState.asStateFlow()

    private val _lookupState = MutableStateFlow<ECourtLookupUiState>(ECourtLookupUiState.Loading)
    val lookupState: StateFlow<ECourtLookupUiState> = _lookupState.asStateFlow()

    private val _captchaUrl = MutableStateFlow("")
    val captchaUrl: StateFlow<String> = _captchaUrl.asStateFlow()

    private var sessionToken: String? = null

    val recentEntries: StateFlow<com.vakildiary.app.domain.model.ECourtRecentEntries> = trackingStore.recentEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.vakildiary.app.domain.model.ECourtRecentEntries(emptyList(), emptyList(), emptyList(), emptyList()))

    init {
        loadSession()
    }

    fun loadSession() {
        viewModelScope.launch {
            _lookupState.value = ECourtLookupUiState.Loading
            when (val result = fetchSessionUseCase()) {
                is Result.Success -> {
                    sessionToken = result.data.token
                    _lookupState.value = ECourtLookupUiState.Success(
                        ECourtLookupData(states = result.data.states)
                    )
                    refreshCaptcha()
                }
                is Result.Error -> {
                    _lookupState.value = ECourtLookupUiState.Error(result.message)
                }
            }
        }
    }

    fun refreshCaptcha() {
        val token = sessionToken ?: return
        viewModelScope.launch {
            when (val result = fetchCaptchaUseCase(token)) {
                is Result.Success -> {
                    sessionToken = result.data.token
                    _captchaUrl.value = result.data.imageUrl
                    trackingStore.saveLastAppToken(result.data.token)
                }
                is Result.Error -> Unit
            }
        }
    }

    fun loadDistricts(stateCode: String) {
        val token = sessionToken ?: return
        viewModelScope.launch {
            updateLookupState(
                result = fetchDistrictsUseCase(token, stateCode),
                onSuccess = { data, newToken ->
                    sessionToken = newToken
                    ECourtLookupData(
                        states = currentStates(),
                        districts = data
                    )
                }
            )
        }
    }

    fun loadCourtComplexes(stateCode: String, districtCode: String) {
        val token = sessionToken ?: return
        viewModelScope.launch {
            updateLookupState(
                result = fetchCourtComplexesUseCase(token, stateCode, districtCode),
                onSuccess = { data, newToken ->
                    sessionToken = newToken
                    ECourtLookupData(
                        states = currentStates(),
                        districts = currentDistricts(),
                        courts = data
                    )
                }
            )
        }
    }

    fun loadEstablishments(stateCode: String, districtCode: String, courtComplexCode: String) {
        val token = sessionToken ?: return
        viewModelScope.launch {
            updateLookupState(
                result = fetchEstablishmentsUseCase(token, stateCode, districtCode, courtComplexCode),
                onSuccess = { data, newToken ->
                    sessionToken = newToken
                    ECourtLookupData(
                        states = currentStates(),
                        districts = currentDistricts(),
                        courts = currentCourts(),
                        establishments = data,
                        requiresEstablishment = true
                    )
                }
            )
        }
    }

    fun loadCaseTypes(stateCode: String, districtCode: String, courtComplexCode: String, establishmentCode: String?) {
        val token = sessionToken ?: return
        viewModelScope.launch {
            updateLookupState(
                result = fetchCaseTypesUseCase(token, stateCode, districtCode, courtComplexCode, establishmentCode),
                onSuccess = { data, newToken ->
                    sessionToken = newToken
                    ECourtLookupData(
                        states = currentStates(),
                        districts = currentDistricts(),
                        courts = currentCourts(),
                        establishments = currentEstablishments(),
                        caseTypes = data,
                        requiresEstablishment = establishmentCode != null
                    )
                }
            )
        }
    }

    fun search(form: ECourtSearchForm) {
        val requiresEstablishment = (lookupState.value as? ECourtLookupUiState.Success)
            ?.data
            ?.requiresEstablishment == true
        if (form.stateCode.isBlank() ||
            form.districtCode.isBlank() ||
            form.courtCode.isBlank() ||
            (requiresEstablishment && form.establishmentCode.isBlank()) ||
            form.caseType.isBlank() ||
            form.caseNumber.isBlank() ||
            form.year.isBlank() ||
            form.captcha.isBlank()
        ) {
            _uiState.value = ECourtSearchUiState.Error("Please fill all required fields")
            return
        }
        val token = sessionToken
        if (token.isNullOrBlank()) {
            _uiState.value = ECourtSearchUiState.Error("eCourt session not ready. Refresh and try again.")
            return
        }
        viewModelScope.launch {
            _uiState.value = ECourtSearchUiState.Loading
            val result = searchECourtUseCase(
                token = token,
                stateCode = form.stateCode.trim(),
                districtCode = form.districtCode.trim(),
                courtComplexCode = form.courtCode.trim(),
                establishmentCode = form.establishmentCode.trim().ifBlank { null },
                caseType = form.caseType.trim(),
                caseNumber = form.caseNumber.trim(),
                year = form.year.trim(),
                captcha = form.captcha.trim()
            )
            _uiState.value = when (result) {
                is Result.Success -> {
                    sessionToken = result.data.token
                    trackingStore.saveLastAppToken(result.data.token)
                    trackingStore.saveRecentEntries(
                        stateCode = form.stateCode.trim(),
                        districtCode = form.districtCode.trim(),
                        courtCode = form.courtCode.trim(),
                        caseTypeCode = form.caseType.trim()
                    )
                    trackingStore.saveLastCaptcha(form.captcha.trim(), null)
                    result.data.captchaImageUrl?.let { _captchaUrl.value = it }
                    val parsed = ECourtParser.parse(result.data.caseHtml, form)
                    handleStatusUpdates(parsed, form)
                    ECourtSearchUiState.Success(parsed)
                }
                is Result.Error -> ECourtSearchUiState.Error(result.message)
            }
        }
    }

    private suspend fun handleStatusUpdates(
        results: List<ECourtCaseItem>,
        form: ECourtSearchForm
    ) {
        for (item in results) {
            val caseResult = getCaseByNumberUseCase(item.caseNumber)
            val existing = (caseResult as? Result.Success)?.data ?: continue
            if (!existing.isECourtTracked) continue

            val newStage = parseStage(item.stage) ?: existing.caseStage
            val newNextDate = parseDate(item.nextHearingDate) ?: existing.nextHearingDate
            val stageChanged = newStage != existing.caseStage
            val dateChanged = newNextDate != null && newNextDate != existing.nextHearingDate

            if (stageChanged || dateChanged) {
                updateCaseUseCase(existing.copy(caseStage = newStage, nextHearingDate = newNextDate))
                trackingStore.save(
                    existing.caseId,
                    com.vakildiary.app.domain.model.ECourtTrackingInfo(
                        stateCode = form.stateCode.trim(),
                        districtCode = form.districtCode.trim(),
                        courtCode = form.courtCode.trim(),
                        establishmentCode = form.establishmentCode.trim().ifBlank { null },
                        caseTypeCode = form.caseType.trim(),
                        caseNumber = item.caseNumber,
                        year = form.year.trim(),
                        courtName = form.courtName,
                        courtType = form.courtType,
                        lastStage = item.stage,
                        lastNextDate = item.nextHearingDate
                    )
                )
                val message = buildString {
                    if (stageChanged) append("Stage: ${existing.caseStage.name} → ${newStage.name}")
                    if (dateChanged) {
                        if (isNotEmpty()) append(" • ")
                        append("Next: ${item.nextHearingDate}")
                    }
                }.ifBlank { "Case status updated" }
                notifier.notifyStatusChange(existing.caseId, existing.caseName, message)
            } else {
                trackingStore.save(
                    existing.caseId,
                    com.vakildiary.app.domain.model.ECourtTrackingInfo(
                        stateCode = form.stateCode.trim(),
                        districtCode = form.districtCode.trim(),
                        courtCode = form.courtCode.trim(),
                        establishmentCode = form.establishmentCode.trim().ifBlank { null },
                        caseTypeCode = form.caseType.trim(),
                        caseNumber = item.caseNumber,
                        year = form.year.trim(),
                        courtName = form.courtName,
                        courtType = form.courtType,
                        lastStage = item.stage,
                        lastNextDate = item.nextHearingDate
                    )
                )
            }
        }
    }

    private fun <T> updateLookupState(
        result: Result<ECourtTokenResult<List<T>>>,
        onSuccess: (List<T>, String) -> ECourtLookupData
    ) {
        _lookupState.value = when (result) {
            is Result.Success -> ECourtLookupUiState.Success(onSuccess(result.data.data, result.data.token))
            is Result.Error -> ECourtLookupUiState.Error(result.message)
        }
    }

    private fun currentStates(): List<ECourtOption> {
        return (lookupState.value as? ECourtLookupUiState.Success)?.data?.states.orEmpty()
    }

    private fun currentDistricts(): List<ECourtOption> {
        return (lookupState.value as? ECourtLookupUiState.Success)?.data?.districts.orEmpty()
    }

    private fun currentCourts(): List<ECourtComplexOption> {
        return (lookupState.value as? ECourtLookupUiState.Success)?.data?.courts.orEmpty()
    }

    private fun currentEstablishments(): List<ECourtOption> {
        return (lookupState.value as? ECourtLookupUiState.Success)?.data?.establishments.orEmpty()
    }

    private fun parseStage(stage: String): CaseStage? {
        val normalized = stage.lowercase()
        return when {
            normalized.contains("disposed") -> CaseStage.DISPOSED
            normalized.contains("judgment") -> CaseStage.JUDGMENT
            normalized.contains("argument") -> CaseStage.ARGUMENTS
            normalized.contains("filing") -> CaseStage.FILING
            normalized.contains("hearing") -> CaseStage.HEARING
            else -> null
        }
    }

    private fun parseDate(value: String): Long? {
        if (value.isBlank()) return null
        return try {
            val parts = value.split("/")
            if (parts.size != 3) return null
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            LocalDate.of(year, month, day)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        } catch (t: Throwable) {
            null
        }
    }
}

data class ECourtLookupData(
    val states: List<ECourtOption> = emptyList(),
    val districts: List<ECourtOption> = emptyList(),
    val courts: List<ECourtComplexOption> = emptyList(),
    val establishments: List<ECourtOption> = emptyList(),
    val caseTypes: List<ECourtOption> = emptyList(),
    val requiresEstablishment: Boolean = false
)

sealed interface ECourtLookupUiState {
    object Loading : ECourtLookupUiState
    data class Success(val data: ECourtLookupData) : ECourtLookupUiState
    data class Error(val message: String) : ECourtLookupUiState
}

sealed interface ECourtSearchUiState {
    object Loading : ECourtSearchUiState
    data class Success(val results: List<ECourtCaseItem>) : ECourtSearchUiState
    data class Error(val message: String) : ECourtSearchUiState
}

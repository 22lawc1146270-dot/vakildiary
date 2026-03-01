package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.ecourt.ECourtTrackingStore
import com.vakildiary.app.domain.model.ECourtComplexOption
import com.vakildiary.app.domain.model.ECourtOption
import com.vakildiary.app.domain.model.ECourtTokenResult
import com.vakildiary.app.domain.model.ECourtTrackedCase
import com.vakildiary.app.domain.usecase.ecourt.GetECourtTrackedCasesUseCase
import com.vakildiary.app.domain.usecase.ecourt.UpsertECourtTrackedCaseUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtCaptchaUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtCaseTypesUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtCourtComplexesUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtDistrictsUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtEstablishmentsUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtSessionUseCase
import com.vakildiary.app.domain.usecase.ecourt.FetchECourtCaseDetailsUseCase
import com.vakildiary.app.domain.usecase.ecourt.SearchECourtUseCase
import com.vakildiary.app.presentation.model.ECourtCaseDetails
import com.vakildiary.app.presentation.model.ECourtCaseItem
import com.vakildiary.app.presentation.model.ECourtDetailParser
import com.vakildiary.app.presentation.model.ECourtParser
import com.vakildiary.app.presentation.model.ECourtSearchForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val fetchCaseDetailsUseCase: FetchECourtCaseDetailsUseCase,
    private val trackingStore: ECourtTrackingStore,
    private val getTrackedCasesUseCase: GetECourtTrackedCasesUseCase,
    private val upsertTrackedCaseUseCase: UpsertECourtTrackedCaseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ECourtSearchUiState>(ECourtSearchUiState.Success(emptyList()))
    val uiState: StateFlow<ECourtSearchUiState> = _uiState.asStateFlow()

    private val _lookupState = MutableStateFlow<ECourtLookupUiState>(ECourtLookupUiState.Loading)
    val lookupState: StateFlow<ECourtLookupUiState> = _lookupState.asStateFlow()

    private val _captchaUrl = MutableStateFlow("")
    val captchaUrl: StateFlow<String> = _captchaUrl.asStateFlow()
    private val _captchaImage = MutableStateFlow<ByteArray?>(null)
    val captchaImage: StateFlow<ByteArray?> = _captchaImage.asStateFlow()

    private var sessionToken: String? = null
    private var lastSearchForm: ECourtSearchForm? = null
    private var lastSearchHtml: String? = null

    private val _detailState = MutableStateFlow<ECourtDetailUiState>(ECourtDetailUiState.Loading)
    val detailState: StateFlow<ECourtDetailUiState> = _detailState.asStateFlow()
    private val _selectedItem = MutableStateFlow<ECourtCaseItem?>(null)
    val selectedItem: StateFlow<ECourtCaseItem?> = _selectedItem.asStateFlow()

    private val _trackEvents = MutableStateFlow<Result<Unit>?>(null)
    val trackEvents: StateFlow<Result<Unit>?> = _trackEvents.asStateFlow()

    val recentEntries: StateFlow<com.vakildiary.app.domain.model.ECourtRecentEntries> = trackingStore.recentEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.vakildiary.app.domain.model.ECourtRecentEntries(emptyList(), emptyList(), emptyList(), emptyList()))

    val trackedCases: StateFlow<ECourtTrackedUiState> = getTrackedCasesUseCase()
        .map { result ->
            when (result) {
                is Result.Success -> ECourtTrackedUiState.Success(result.data)
                is Result.Error -> ECourtTrackedUiState.Error(result.message)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ECourtTrackedUiState.Loading)

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
                    _captchaImage.value = result.data.imageBytes
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
        val normalizedCaptcha = form.captcha.filterNot { it.isWhitespace() }
        if (form.stateCode.isBlank() ||
            form.districtCode.isBlank() ||
            form.courtCode.isBlank() ||
            (requiresEstablishment && form.establishmentCode.isBlank()) ||
            form.caseType.isBlank() ||
            form.caseNumber.isBlank() ||
            form.year.isBlank() ||
            normalizedCaptcha.isBlank()
        ) {
            _uiState.value = ECourtSearchUiState.Error("Please fill all required fields")
            return
        }
        val token = sessionToken
        if (token == null) {
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
                captcha = normalizedCaptcha
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
                    trackingStore.saveLastCaptcha(normalizedCaptcha, null)
                    result.data.captchaImageUrl?.let { _captchaUrl.value = it }
                    result.data.captchaImageBytes?.let { _captchaImage.value = it }
                    val parsed = ECourtParser.parse(result.data.caseHtml, form)
                    lastSearchHtml = result.data.caseHtml
                    lastSearchForm = form
                    ECourtSearchUiState.Success(parsed)
                }
                is Result.Error -> {
                    lastSearchHtml = null
                    if (result.message.contains("captcha", ignoreCase = true)) {
                        refreshCaptcha()
                    }
                    ECourtSearchUiState.Error(result.message)
                }
            }
        }
    }

    fun selectDetails(item: ECourtCaseItem) {
        _selectedItem.value = item
        val detailLink = item.detailLink
        if (detailLink.isNullOrBlank()) {
            val parsedFromSearch = lastSearchHtml?.let { ECourtDetailParser.parse(it, item) }
            val hasParsedData = parsedFromSearch?.let { details ->
                details.caseDetails.isNotEmpty() ||
                    details.caseStatus.isNotEmpty() ||
                    details.petitionerAdvocate.isNotEmpty() ||
                    details.respondentAdvocate.isNotEmpty() ||
                    details.acts.isNotEmpty() ||
                    details.caseHistory.isNotEmpty() ||
                    details.transferDetails.isNotEmpty()
            } == true
            _detailState.value = ECourtDetailUiState.Success(
                if (hasParsedData) {
                    parsedFromSearch!!
                } else {
                    ECourtCaseDetails(
                        caseTitle = item.caseTitle,
                        caseNumber = item.caseNumber,
                        courtName = item.courtName,
                        courtType = item.courtType,
                        parties = item.parties,
                        stage = item.stage.takeIf { it.isNotBlank() },
                        nextHearingDate = item.nextHearingDate.takeIf { it.isNotBlank() },
                        caseDetails = emptyList(),
                        caseStatus = emptyList(),
                        petitionerAdvocate = emptyList(),
                        respondentAdvocate = emptyList(),
                        acts = emptyList(),
                        caseHistory = emptyList(),
                        transferDetails = emptyList()
                    )
                }
            )
            return
        }
        _detailState.value = ECourtDetailUiState.Loading
        viewModelScope.launch {
            val token = sessionToken ?: trackingStore.getLastAppToken().orEmpty()
            _detailState.value = when (val result = fetchCaseDetailsUseCase(token, detailLink)) {
                is Result.Success -> ECourtDetailUiState.Success(ECourtDetailParser.parse(result.data, item))
                is Result.Error -> ECourtDetailUiState.Error(result.message)
            }
        }
    }

    fun selectTrackedCase(case: ECourtTrackedCase) {
        _selectedItem.value = ECourtCaseItem(
            caseNumber = case.caseNumber,
            caseTitle = case.caseTitle,
            parties = case.parties,
            nextHearingDate = case.nextHearingDate.orEmpty(),
            stage = case.stage.orEmpty(),
            courtName = case.courtName,
            courtType = case.courtType,
            clientName = ""
        )
        val details = ECourtCaseDetails(
            caseTitle = case.caseTitle,
            caseNumber = case.caseNumber,
            courtName = case.courtName,
            courtType = case.courtType,
            parties = case.parties,
            stage = case.stage,
            nextHearingDate = case.nextHearingDate,
            caseDetails = case.caseDetails,
            caseStatus = case.caseStatus,
            petitionerAdvocate = case.petitionerAdvocate,
            respondentAdvocate = case.respondentAdvocate,
            acts = case.acts,
            caseHistory = case.caseHistory,
            transferDetails = case.transferDetails
        )
        _detailState.value = ECourtDetailUiState.Success(details)
    }

    fun clearSelection() {
        _selectedItem.value = null
        _detailState.value = ECourtDetailUiState.Loading
    }

    fun trackSelectedCase() {
        val item = _selectedItem.value ?: return
        val details = (_detailState.value as? ECourtDetailUiState.Success)?.details ?: return
        val form = lastSearchForm ?: return
        val trackId = buildTrackId(item.caseNumber, form.year, form.courtName)
        val tracked = ECourtTrackedCase(
            trackId = trackId,
            caseTitle = item.caseTitle,
            caseNumber = item.caseNumber,
            year = form.year,
            courtName = form.courtName,
            courtType = form.courtType,
            parties = item.parties,
            stage = item.stage.takeIf { it.isNotBlank() },
            nextHearingDate = item.nextHearingDate.takeIf { it.isNotBlank() },
            caseDetails = details.caseDetails,
            caseStatus = details.caseStatus,
            petitionerAdvocate = details.petitionerAdvocate,
            respondentAdvocate = details.respondentAdvocate,
            acts = details.acts,
            caseHistory = details.caseHistory,
            transferDetails = details.transferDetails,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            _trackEvents.value = upsertTrackedCaseUseCase(tracked)
        }
    }

    fun clearTrackEvent() {
        _trackEvents.value = null
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

    private fun buildTrackId(caseNumber: String, year: String, courtName: String): String {
        val raw = "${caseNumber.trim()}_${year.trim()}_${courtName.trim()}".lowercase()
        return raw.replace(Regex("[^a-z0-9_]+"), "_").trim('_')
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

sealed interface ECourtDetailUiState {
    object Loading : ECourtDetailUiState
    data class Success(val details: ECourtCaseDetails) : ECourtDetailUiState
    data class Error(val message: String) : ECourtDetailUiState
}

sealed interface ECourtTrackedUiState {
    object Loading : ECourtTrackedUiState
    data class Success(val cases: List<ECourtTrackedCase>) : ECourtTrackedUiState
    data class Error(val message: String) : ECourtTrackedUiState
}

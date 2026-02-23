package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.ecourt.ECourtTrackingStore
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.ECourtRecentEntries
import com.vakildiary.app.domain.usecase.ecourt.SearchECourtUseCase
import com.vakildiary.app.domain.usecase.cases.GetCaseByNumberUseCase
import com.vakildiary.app.domain.usecase.cases.UpdateCaseUseCase
import com.vakildiary.app.presentation.model.ECourtCaseItem
import com.vakildiary.app.presentation.model.ECourtSearchForm
import com.vakildiary.app.presentation.model.ECourtParser
import com.vakildiary.app.notifications.ECourtStatusNotifier
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
    private val getCaseByNumberUseCase: GetCaseByNumberUseCase,
    private val updateCaseUseCase: UpdateCaseUseCase,
    private val trackingStore: ECourtTrackingStore,
    private val notifier: ECourtStatusNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow<ECourtSearchUiState>(ECourtSearchUiState.Success(emptyList()))
    val uiState: StateFlow<ECourtSearchUiState> = _uiState.asStateFlow()

    private val _captchaUrl = MutableStateFlow(buildCaptchaUrl())
    val captchaUrl: StateFlow<String> = _captchaUrl.asStateFlow()

    val recentEntries: StateFlow<ECourtRecentEntries> = trackingStore.recentEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ECourtRecentEntries(emptyList(), emptyList(), emptyList(), emptyList()))

    fun refreshCaptcha() {
        _captchaUrl.value = buildCaptchaUrl()
    }

    fun search(form: ECourtSearchForm) {
        if (form.stateCode.isBlank() ||
            form.districtCode.isBlank() ||
            form.courtCode.isBlank() ||
            form.caseType.isBlank() ||
            form.caseNumber.isBlank() ||
            form.year.isBlank() ||
            form.captcha.isBlank()
        ) {
            _uiState.value = ECourtSearchUiState.Error("Please fill all required fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = ECourtSearchUiState.Loading
            val result = searchECourtUseCase(
                stateCode = form.stateCode.trim(),
                districtCode = form.districtCode.trim(),
                courtCode = form.courtCode.trim(),
                caseType = form.caseType.trim(),
                caseNumber = form.caseNumber.trim(),
                year = form.year.trim(),
                captcha = form.captcha.trim(),
                csrfMagic = form.csrfMagic.trim().ifBlank { null }
            )
            _uiState.value = when (result) {
                is Result.Success -> {
                    val parsed = ECourtParser.parse(result.data, form)
                    trackingStore.saveRecentEntries(
                        stateCode = form.stateCode.trim(),
                        districtCode = form.districtCode.trim(),
                        courtCode = form.courtCode.trim(),
                        caseTypeCode = form.caseType.trim()
                    )
                    trackingStore.saveLastCaptcha(form.captcha.trim(), form.csrfMagic.trim().ifBlank { null })
                    handleStatusUpdates(parsed, form)
                    ECourtSearchUiState.Success(parsed)
                }
                is Result.Error -> ECourtSearchUiState.Error(result.message)
            }
            refreshCaptcha()
        }
    }

    private suspend fun handleStatusUpdates(
        results: List<com.vakildiary.app.presentation.model.ECourtCaseItem>,
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

    private fun buildCaptchaUrl(): String {
        return "$CAPTCHA_URL?ts=${System.currentTimeMillis()}"
    }

    companion object {
        private const val CAPTCHA_URL = "https://hcservices.ecourts.gov.in/ecourtindiaHC/cases/captcha.php"
    }
}

sealed interface ECourtSearchUiState {
    object Loading : ECourtSearchUiState
    data class Success(val results: List<ECourtCaseItem>) : ECourtSearchUiState
    data class Error(val message: String) : ECourtSearchUiState
}

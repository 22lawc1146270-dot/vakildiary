package com.vakildiary.app.presentation.viewmodels

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.DocumentTags
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.remote.reportable.ReportableBackendService
import com.vakildiary.app.data.remote.reportable.ReportableSubmitRequestDto
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.usecase.documents.AttachDocumentUseCase
import com.vakildiary.app.presentation.model.ReportableCaseTypeOption
import com.vakildiary.app.presentation.model.ReportableFormData
import com.vakildiary.app.presentation.model.ReportableFormInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ReportableJudgmentViewModel @Inject constructor(
    private val backendService: ReportableBackendService,
    private val attachDocumentUseCase: AttachDocumentUseCase
) : ViewModel() {
    private val _formState = MutableStateFlow<ReportableFormUiState>(ReportableFormUiState.Loading)
    val formState: StateFlow<ReportableFormUiState> = _formState.asStateFlow()

    private val _downloadState = MutableStateFlow<ReportableDownloadUiState>(ReportableDownloadUiState.Idle)
    val downloadState: StateFlow<ReportableDownloadUiState> = _downloadState.asStateFlow()

    fun loadForm(caseNumber: String?, year: String?) {
        viewModelScope.launch {
            _formState.value = ReportableFormUiState.Loading
            _formState.value = when (val result = fetchFormData(caseNumber, year)) {
                is Result.Success -> ReportableFormUiState.Success(result.data)
                is Result.Error -> ReportableFormUiState.Error(result.message)
            }
        }
    }

    fun submitForm(
        input: ReportableFormInput,
        judgmentId: String,
        year: String?,
        caseNumber: String?,
        requiredMessage: String
    ) {
        val current = (_formState.value as? ReportableFormUiState.Success)?.data
            ?: return
        if (input.sessionId.isBlank() ||
            input.caseTypeValue.isBlank() ||
            input.caseNumber.isBlank() ||
            input.captcha.isBlank() ||
            (current.requiresCaseYear && input.caseYear.isNullOrBlank()) ||
            (current.requiresDiaryYear && input.diaryYear.isNullOrBlank())
        ) {
            _downloadState.value = ReportableDownloadUiState.Error(requiredMessage)
            return
        }
        if (_downloadState.value is ReportableDownloadUiState.Loading) return
        viewModelScope.launch {
            _downloadState.value = ReportableDownloadUiState.Loading
            _downloadState.value = withContext(Dispatchers.IO) {
                when (val result = submitFormRequest(input)) {
                    is Result.Success -> downloadReportablePdf(
                        downloadUrl = result.data.downloadUrl,
                        fileName = result.data.fileName,
                        judgmentId = judgmentId,
                        year = year,
                        caseNumber = caseNumber
                    )
                    is Result.Error -> ReportableDownloadUiState.Error(result.message)
                }
            }
        }
    }

    fun resetState() {
        _downloadState.value = ReportableDownloadUiState.Idle
    }

    private suspend fun fetchFormData(
        caseNumber: String?,
        year: String?
    ): Result<ReportableFormData> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = backendService.fetchForm(caseNumber, year)
            val caseTypes = response.caseTypes.map {
                ReportableCaseTypeOption(value = it.value, label = it.label)
            }
            val captchaBytes = Base64.decode(response.captchaImageBase64, Base64.DEFAULT)
            val suggested = response.suggestedCaseTypeValue
                ?: suggestCaseTypeValue(caseNumber, caseTypes)
            Result.Success(
                ReportableFormData(
                    sessionId = response.sessionId,
                    caseTypes = caseTypes,
                    requiresCaseYear = response.requiresCaseYear,
                    requiresDiaryYear = response.requiresDiaryYear,
                    captchaImage = captchaBytes,
                    suggestedCaseTypeValue = suggested
                )
            )
        } catch (t: Throwable) {
            Result.Error("Failed to load reportable form", t)
        }
    }

    private suspend fun submitFormRequest(
        input: ReportableFormInput
    ): Result<com.vakildiary.app.data.remote.reportable.ReportableSubmitResponseDto> {
        return try {
            val response = backendService.submitForm(
                ReportableSubmitRequestDto(
                    sessionId = input.sessionId,
                    caseTypeValue = input.caseTypeValue,
                    caseNumber = input.caseNumber,
                    caseYear = input.caseYear,
                    diaryYear = input.diaryYear,
                    captcha = input.captcha
                )
            )
            Result.Success(response)
        } catch (t: Throwable) {
            Result.Error("Reportable submission failed", t)
        }
    }

    private suspend fun downloadReportablePdf(
        downloadUrl: String,
        fileName: String?,
        judgmentId: String,
        year: String?,
        caseNumber: String?
    ): ReportableDownloadUiState {
        return try {
            val body = backendService.downloadReportable(downloadUrl)
            val safeFileName = fileName ?: "reportable_${judgmentId}.pdf"
            val mimeType = body.contentType()?.toString() ?: "application/pdf"
            val tags = DocumentTags.buildJudgmentTags(
                judgmentId = judgmentId,
                year = year,
                caseNumber = caseNumber,
                reportable = true
            )
            return when (val result = attachDocumentUseCase(
                caseId = null,
                fileName = safeFileName,
                mimeType = mimeType,
                inputStreamProvider = { body.byteStream() },
                isScanned = false,
                tags = tags
            )) {
                is Result.Success -> ReportableDownloadUiState.Success(result.data)
                is Result.Error -> ReportableDownloadUiState.Error(result.message)
            }
        } catch (t: Throwable) {
            ReportableDownloadUiState.Error("Reportable download failed")
        }
    }

    private fun suggestCaseTypeValue(
        caseNumber: String?,
        options: List<ReportableCaseTypeOption>
    ): String? {
        val label = extractCaseTypeLabel(caseNumber) ?: return null
        return options.firstOrNull { it.label.contains(label, ignoreCase = true) }?.value
    }

    private fun extractCaseTypeLabel(caseNumber: String?): String? {
        if (caseNumber.isNullOrBlank()) return null
        val normalized = caseNumber.replace(Regex("\\s+"), " ").trim()
        val match = Regex(
            "([A-Za-z][A-Za-z\\s&.-]+)\\s*(No\\.|No|Nos\\.|Nos|Number)",
            RegexOption.IGNORE_CASE
        ).find(normalized)
        return match?.groupValues?.getOrNull(1)?.trim()
    }
}

sealed interface ReportableFormUiState {
    object Loading : ReportableFormUiState
    data class Success(val data: ReportableFormData) : ReportableFormUiState
    data class Error(val message: String) : ReportableFormUiState
}

sealed interface ReportableDownloadUiState {
    object Idle : ReportableDownloadUiState
    object Loading : ReportableDownloadUiState
    data class Success(val document: Document) : ReportableDownloadUiState
    data class Error(val message: String) : ReportableDownloadUiState
}

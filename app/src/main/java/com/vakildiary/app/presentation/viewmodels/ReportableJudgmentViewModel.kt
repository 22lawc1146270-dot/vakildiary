package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.DocumentTags
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.usecase.documents.AttachDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@HiltViewModel
class ReportableJudgmentViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val attachDocumentUseCase: AttachDocumentUseCase
) : ViewModel() {
    private val _downloadState = MutableStateFlow<ReportableDownloadUiState>(ReportableDownloadUiState.Idle)
    val downloadState: StateFlow<ReportableDownloadUiState> = _downloadState.asStateFlow()

    fun downloadReportable(
        url: String,
        userAgent: String?,
        mimeType: String?,
        cookies: String?,
        judgmentId: String,
        year: String?,
        caseNumber: String?,
        fileName: String
    ) {
        if (_downloadState.value is ReportableDownloadUiState.Loading) return
        viewModelScope.launch {
            _downloadState.value = ReportableDownloadUiState.Loading
            _downloadState.value = withContext(Dispatchers.IO) {
                val requestBuilder = Request.Builder().url(url)
                if (!cookies.isNullOrBlank()) {
                    requestBuilder.header("Cookie", cookies)
                }
                if (!userAgent.isNullOrBlank()) {
                    requestBuilder.header("User-Agent", userAgent)
                }
                val request = requestBuilder.build()
                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withContext ReportableDownloadUiState.Error(
                                "Reportable download failed (HTTP ${response.code})"
                            )
                        }
                        val body = response.body ?: return@withContext ReportableDownloadUiState.Error(
                            "Reportable download failed"
                        )
                        val resolvedType = mimeType ?: body.contentType()?.toString() ?: "application/pdf"
                        val tags = DocumentTags.buildJudgmentTags(
                            judgmentId = judgmentId,
                            year = year,
                            caseNumber = caseNumber,
                            reportable = true
                        )
                        return@withContext when (val result = attachDocumentUseCase(
                            caseId = null,
                            fileName = fileName,
                            mimeType = resolvedType,
                            inputStreamProvider = { body.byteStream() },
                            isScanned = false,
                            tags = tags
                        )) {
                            is Result.Success -> ReportableDownloadUiState.Success(result.data)
                            is Result.Error -> ReportableDownloadUiState.Error(result.message)
                        }
                    }
                } catch (t: Throwable) {
                    return@withContext ReportableDownloadUiState.Error("Reportable download failed")
                }
            }
        }
    }

    fun resetState() {
        _downloadState.value = ReportableDownloadUiState.Idle
    }
}

sealed interface ReportableDownloadUiState {
    object Idle : ReportableDownloadUiState
    object Loading : ReportableDownloadUiState
    data class Success(val document: Document) : ReportableDownloadUiState
    data class Error(val message: String) : ReportableDownloadUiState
}

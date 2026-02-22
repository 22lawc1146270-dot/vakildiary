package com.vakildiary.app.presentation.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.usecase.cases.GetAllCasesUseCase
import com.vakildiary.app.domain.usecase.documents.AttachDocumentUseCase
import com.vakildiary.app.domain.usecase.documents.DeleteDocumentUseCase
import com.vakildiary.app.domain.usecase.documents.GetAllDocumentsUseCase
import com.vakildiary.app.domain.usecase.documents.GetDocumentsByCaseIdUseCase
import com.vakildiary.app.domain.usecase.documents.MoveDocumentUseCase
import com.vakildiary.app.domain.usecase.documents.PrepareDocumentForViewingUseCase
import com.vakildiary.app.domain.usecase.documents.RenameDocumentUseCase
import com.vakildiary.app.domain.usecase.documents.SaveScannedDocumentUseCase
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import com.vakildiary.app.presentation.viewmodels.state.DocumentListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val getDocumentsByCaseIdUseCase: GetDocumentsByCaseIdUseCase,
    private val attachDocumentUseCase: AttachDocumentUseCase,
    private val saveScannedDocumentUseCase: SaveScannedDocumentUseCase,
    private val renameDocumentUseCase: RenameDocumentUseCase,
    private val moveDocumentUseCase: MoveDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val prepareDocumentForViewingUseCase: PrepareDocumentForViewingUseCase,
    private val getAllCasesUseCase: GetAllCasesUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _fileEvents = MutableSharedFlow<Result<File>>(extraBufferCapacity = 1)
    val fileEvents = _fileEvents

    private val _previewMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val previewMap: StateFlow<Map<String, String>> = _previewMap.asStateFlow()

    val casesState: StateFlow<CasePickerUiState> = getAllCasesUseCase()
        .map { result ->
            when (result) {
                is Result.Success -> CasePickerUiState.Success(result.data)
                is Result.Error -> CasePickerUiState.Error(result.message)
            }
        }
        .catch { emit(CasePickerUiState.Error("Failed to load cases")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CasePickerUiState.Loading)

    fun uiState(caseId: String?): StateFlow<DocumentListUiState> {
        val flow = if (caseId.isNullOrBlank()) {
            getAllDocumentsUseCase()
        } else {
            getDocumentsByCaseIdUseCase(caseId)
        }
        return flow
            .map { result ->
                when (result) {
                    is Result.Success -> DocumentListUiState.Success(result.data)
                    is Result.Error -> DocumentListUiState.Error(result.message)
                }
            }
            .catch { emit(DocumentListUiState.Error("Failed to load documents")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentListUiState.Loading)
    }

    fun attachDocumentFromUri(caseId: String?, uri: Uri) {
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(uri) ?: "document_${System.currentTimeMillis()}"
            val input = openInputStream(uri)
            if (input == null) {
                _fileEvents.tryEmit(Result.Error("Unable to read file"))
                return@launch
            }
            val result = attachDocumentUseCase(
                caseId = caseId,
                fileName = fileName,
                mimeType = mimeType,
                inputStreamProvider = { input },
                isScanned = false,
                tags = ""
            )
            if (result is Result.Error) {
                _fileEvents.tryEmit(Result.Error(result.message))
            }
        }
    }

    fun attachScannedDocument(caseId: String?, uri: Uri) {
        viewModelScope.launch {
            val fileName = "scan_${System.currentTimeMillis()}.pdf"
            val input = openInputStream(uri)
            if (input == null) {
                _fileEvents.tryEmit(Result.Error("Unable to read scan"))
                return@launch
            }
            val result = saveScannedDocumentUseCase(
                caseId = caseId,
                fileName = fileName,
                inputStreamProvider = { input }
            )
            if (result is Result.Error) {
                _fileEvents.tryEmit(Result.Error(result.message))
            }
        }
    }

    fun renameDocument(document: Document, newFileName: String) {
        viewModelScope.launch {
            renameDocumentUseCase(document, newFileName)
        }
    }

    fun moveDocument(document: Document, newCaseId: String?) {
        viewModelScope.launch {
            moveDocumentUseCase(document, newCaseId)
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            deleteDocumentUseCase(document)
        }
    }

    fun prepareFileForViewing(document: Document) {
        viewModelScope.launch {
            val result = prepareDocumentForViewingUseCase(document.filePath)
            _fileEvents.emit(result)
        }
    }

    fun loadPreviewIfNeeded(document: Document) {
        if (!document.fileType.startsWith("image")) return
        if (_previewMap.value.containsKey(document.documentId)) return
        viewModelScope.launch {
            when (val result = prepareDocumentForViewingUseCase(document.filePath)) {
                is Result.Success -> {
                    val updated = _previewMap.value.toMutableMap()
                    updated[document.documentId] = result.data.absolutePath
                    _previewMap.value = updated
                }
                is Result.Error -> {
                    // Ignore preview errors.
                }
            }
        }
    }

    private fun openInputStream(uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    private fun getFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1) return@use null
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }
}

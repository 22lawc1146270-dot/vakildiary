package com.vakildiary.app.presentation.screens.documents

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vakildiary.app.core.Result
import com.vakildiary.app.core.ShareUtils
import com.vakildiary.app.data.documents.DocumentScannerManager
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.presentation.viewmodels.DocumentListViewModel
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import com.vakildiary.app.presentation.viewmodels.state.DocumentListUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.content.ContextCompat

private enum class FileAction { OPEN, SHARE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentListScreen(
    caseId: String? = null,
    showTopBar: Boolean = true,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: DocumentListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState(caseId).collectAsStateWithLifecycle()
    val casesState by viewModel.casesState.collectAsStateWithLifecycle()
    val previewMap by viewModel.previewMap.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedDocument by remember { mutableStateOf<Document?>(null) }
    var showActionsSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showMoveDialog by remember { mutableStateOf(false) }
    var selectedMoveCaseId by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<FileAction?>(null) }
    var showLargeFileDialog by remember { mutableStateOf(false) }
    var showVideoWarning by remember { mutableStateOf(false) }
    var pendingLargeFileUri by remember { mutableStateOf<Uri?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val scannerManager = remember { DocumentScannerManager() }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val scanResult = scannerManager.extractResult(result.data)
        val pdfUri = scanResult?.pdf?.uri
        if (pdfUri != null) {
            viewModel.attachScannedDocument(caseId, pdfUri)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val info = getFileInfo(context, uri)
            if (info != null && info.mimeType.startsWith("video") && info.sizeBytes >= VIDEO_WARN_THRESHOLD) {
                showVideoWarning = true
                return@rememberLauncherForActivityResult
            }
            if (info != null && info.sizeBytes >= LARGE_FILE_THRESHOLD) {
                pendingLargeFileUri = uri
                showLargeFileDialog = true
                return@rememberLauncherForActivityResult
            }
            viewModel.attachDocumentFromUri(caseId, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fileEvents.collect { result ->
            when (result) {
                is Result.Success -> {
                    val action = pendingAction
                    val file = result.data
                    val doc = selectedDocument
                    if (file != null && action != null && doc != null) {
                        when (action) {
                            FileAction.OPEN -> ShareUtils.openFile(context, file, doc.fileType)
                            FileAction.SHARE -> ShareUtils.shareFile(context, file, doc.fileType)
                        }
                    }
                    pendingAction = null
                }
                is Result.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(text = if (caseId.isNullOrBlank()) "Documents" else "Case Documents") },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = onBack) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(text = "Search documents") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") }
            )

            if (!hasCameraPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionCard(
                    title = "Camera permission required for scanning",
                    actionLabel = "Grant permission",
                    onAction = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { filePicker.launch("*/*") }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Attach")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Attach")
                }
                Button(onClick = {
                    if (!hasCameraPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        return@Button
                    }
                    val activity = context as? Activity
                    if (activity != null) {
                        scannerManager.getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Scanner unavailable", Toast.LENGTH_SHORT).show()
                            }
                    }
                }) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Scan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Scan")
                }
            }

            when (val state = uiState) {
                DocumentListUiState.Loading -> CircularProgressIndicator()
                is DocumentListUiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error)
                is DocumentListUiState.Success -> {
                    val filtered = state.documents.filter {
                        it.fileName.contains(searchQuery, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        Text(text = "No documents yet")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.documentId }) { doc ->
                                LaunchedEffect(doc.documentId) {
                                    viewModel.loadPreviewIfNeeded(doc)
                                }
                                DocumentRow(
                                    document = doc,
                                    previewPath = previewMap[doc.documentId],
                                    onOpen = {
                                        selectedDocument = doc
                                        pendingAction = FileAction.OPEN
                                        viewModel.prepareFileForViewing(doc)
                                    },
                                    onLongPress = {
                                        selectedDocument = doc
                                        showActionsSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActionsSheet && selectedDocument != null) {
        ModalBottomSheet(onDismissRequest = { showActionsSheet = false }) {
            ListItem(
                headlineContent = { Text(text = "Open") },
                leadingContent = { Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Open") },
                modifier = Modifier.clickable(onClick = {
                    val doc = selectedDocument!!
                    pendingAction = FileAction.OPEN
                    viewModel.prepareFileForViewing(doc)
                    showActionsSheet = false
                })
            )
            ListItem(
                headlineContent = { Text(text = "Share") },
                leadingContent = { Icon(imageVector = Icons.Default.Send, contentDescription = "Share") },
                modifier = Modifier.clickable(onClick = {
                    val doc = selectedDocument!!
                    pendingAction = FileAction.SHARE
                    viewModel.prepareFileForViewing(doc)
                    showActionsSheet = false
                })
            )
            Divider()
            ListItem(
                headlineContent = { Text(text = "Rename") },
                leadingContent = { Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename") },
                modifier = Modifier.clickable(onClick = {
                    renameText = selectedDocument!!.fileName
                    showRenameDialog = true
                    showActionsSheet = false
                })
            )
            ListItem(
                headlineContent = { Text(text = "Move") },
                leadingContent = { Icon(imageVector = Icons.Default.DriveFileMove, contentDescription = "Move") },
                modifier = Modifier.clickable(onClick = {
                    selectedMoveCaseId = selectedDocument!!.caseId
                    showMoveDialog = true
                    showActionsSheet = false
                })
            )
            ListItem(
                headlineContent = { Text(text = "Delete") },
                leadingContent = { Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete") },
                modifier = Modifier.clickable(onClick = {
                    viewModel.deleteDocument(selectedDocument!!)
                    showActionsSheet = false
                })
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showRenameDialog && selectedDocument != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(text = "Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(text = "File name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameDocument(selectedDocument!!, renameText.trim())
                    showRenameDialog = false
                }) { Text(text = "Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(text = "Cancel") }
            }
        )
    }

    if (showMoveDialog && selectedDocument != null) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text(text = "Move to Case") },
            text = {
                when (casesState) {
                    CasePickerUiState.Loading -> CircularProgressIndicator()
                    is CasePickerUiState.Error -> Text(text = (casesState as CasePickerUiState.Error).message)
                    is CasePickerUiState.Success -> {
                        val cases = (casesState as CasePickerUiState.Success).cases
                        CaseDropdown(
                            cases = cases,
                            selectedCaseId = selectedMoveCaseId,
                            onSelectedCaseId = { selectedMoveCaseId = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.moveDocument(selectedDocument!!, selectedMoveCaseId)
                    showMoveDialog = false
                }) { Text(text = "Move") }
            },
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text(text = "Cancel") }
            }
        )
    }

    if (showLargeFileDialog && pendingLargeFileUri != null) {
        AlertDialog(
            onDismissRequest = { showLargeFileDialog = false },
            title = { Text(text = "Large file warning") },
            text = { Text(text = "This file is large and may take longer to upload or share.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.attachDocumentFromUri(caseId, pendingLargeFileUri!!)
                    pendingLargeFileUri = null
                    showLargeFileDialog = false
                }) { Text(text = "Attach") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingLargeFileUri = null
                    showLargeFileDialog = false
                }) { Text(text = "Cancel") }
            }
        )
    }

    if (showVideoWarning) {
        AlertDialog(
            onDismissRequest = { showVideoWarning = false },
            title = { Text(text = "Video too large") },
            text = { Text(text = "Please compress this video before attaching.") },
            confirmButton = {
                TextButton(onClick = { showVideoWarning = false }) { Text(text = "OK") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(
    document: Document,
    previewPath: String?,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (document.fileType.startsWith("image")) {
                AsyncImage(
                    model = previewPath ?: document.thumbnailPath ?: document.filePath,
                    contentDescription = document.fileName,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    imageVector = Icons.Default.Description,
                    contentDescription = document.fileName,
                    modifier = Modifier.size(48.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatMeta(document.fileSizeBytes, document.createdAt),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseDropdown(
    cases: List<Case>,
    selectedCaseId: String?,
    onSelectedCaseId: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = cases.firstOrNull { it.caseId == selectedCaseId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.caseName} • ${it.caseNumber}" }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Case") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = "No Case") },
                onClick = {
                    onSelectedCaseId(null)
                    expanded = false
                }
            )
            cases.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = "${item.caseName} • ${item.caseNumber}") },
                    onClick = {
                        onSelectedCaseId(item.caseId)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatMeta(fileSizeBytes: Long, createdAt: Long): String {
    val size = when {
        fileSizeBytes >= 1_000_000 -> "${fileSizeBytes / 1_000_000} MB"
        fileSizeBytes >= 1_000 -> "${fileSizeBytes / 1_000} KB"
        else -> "$fileSizeBytes B"
    }
    val date = Instant.ofEpochMilli(createdAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    return "$size • $date"
}

@Composable
private fun PermissionCard(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
            Text(text = actionLabel)
        }
    }
}

private data class FileInfo(val sizeBytes: Long, val mimeType: String)

private fun getFileInfo(context: Context, uri: Uri): FileInfo? {
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
    if (size == null || size <= 0) return null
    return FileInfo(sizeBytes = size, mimeType = mimeType)
}

private const val LARGE_FILE_THRESHOLD = 20L * 1024 * 1024
private const val VIDEO_WARN_THRESHOLD = 100L * 1024 * 1024

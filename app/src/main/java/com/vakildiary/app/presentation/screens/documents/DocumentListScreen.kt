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
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vakildiary.app.R
import com.vakildiary.app.core.Result
import com.vakildiary.app.core.DocumentTags
import com.vakildiary.app.core.ShareUtils
import com.vakildiary.app.data.documents.DocumentScannerManager
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.presentation.components.ButtonLabel
import com.vakildiary.app.presentation.viewmodels.DocumentListViewModel
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import com.vakildiary.app.presentation.viewmodels.state.DocumentListUiState
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.components.AppCard
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
    onDownloadReportable: (judgmentId: String, caseNumber: String?, year: String?) -> Unit = { _, _, _ -> },
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
                    title = { Text(text = if (caseId.isNullOrBlank()) "Documents" else "Case Documents", style = VakilTheme.typography.headlineMedium) },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = onBack) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = VakilTheme.colors.textPrimary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = VakilTheme.colors.bgPrimary,
                        titleContentColor = VakilTheme.colors.textPrimary
                    )
                )
            }
        },
        containerColor = VakilTheme.colors.bgPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(VakilTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name...", style = VakilTheme.typography.bodyMedium) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = VakilTheme.colors.textTertiary) },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VakilTheme.colors.accentPrimary,
                    unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft,
                    focusedContainerColor = VakilTheme.colors.bgSecondary,
                    unfocusedContainerColor = VakilTheme.colors.bgSecondary
                ),
                textStyle = VakilTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)
            ) {
                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VakilTheme.colors.accentPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Attach", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    ButtonLabel(text = "Attach", style = VakilTheme.typography.labelMedium)
                }
                Button(
                    onClick = {
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
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VakilTheme.colors.bgElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Scan", modifier = Modifier.size(18.dp), tint = VakilTheme.colors.accentPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    ButtonLabel(text = "Scan", style = VakilTheme.typography.labelMedium)
                }
            }

            when (val state = uiState) {
                DocumentListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VakilTheme.colors.accentPrimary)
                }
                is DocumentListUiState.Error -> Text(text = state.message, color = VakilTheme.colors.error)
                is DocumentListUiState.Success -> {
                    val filtered = state.documents.filter {
                        it.fileName.contains(searchQuery, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "No documents found", style = VakilTheme.typography.bodyLarge, color = VakilTheme.colors.textTertiary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)
                        ) {
                            if (caseId.isNullOrBlank()) {
                                val judgments = filtered.filter { DocumentTags.isJudgment(it.tags) }
                                val others = filtered.filterNot { DocumentTags.isJudgment(it.tags) }
                                val reportable = judgments.filter { DocumentTags.isReportable(it.tags) }
                                val nonReportable = judgments.filterNot { DocumentTags.isReportable(it.tags) }
                                val reportableById = reportable.associateBy { DocumentTags.judgmentId(it.tags) }
                                val nonReportableIds = nonReportable.mapNotNull { DocumentTags.judgmentId(it.tags) }.toSet()
                                val extraReportable = reportable.filter { DocumentTags.judgmentId(it.tags) !in nonReportableIds }
                                val judgmentEntries = nonReportable + extraReportable

                                if (judgmentEntries.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(id = R.string.document_judgments_section),
                                            style = VakilTheme.typography.titleMedium,
                                            color = VakilTheme.colors.textPrimary
                                        )
                                    }
                                    items(judgmentEntries, key = { it.documentId }) { doc ->
                                        LaunchedEffect(doc.documentId) {
                                            viewModel.loadPreviewIfNeeded(doc)
                                        }
                                        val judgmentId = DocumentTags.judgmentId(doc.tags)
                                        val reportableDoc = judgmentId?.let { reportableById[it] }
                                            ?: if (DocumentTags.isReportable(doc.tags)) doc else null
                                        JudgmentDocumentRow(
                                            document = doc,
                                            previewPath = previewMap[doc.documentId],
                                            reportableDocument = reportableDoc,
                                            onOpen = {
                                                selectedDocument = doc
                                                pendingAction = FileAction.OPEN
                                                viewModel.prepareFileForViewing(doc)
                                            },
                                            onLongPress = {
                                                selectedDocument = doc
                                                showActionsSheet = true
                                            },
                                            onDownloadReportable = {
                                                val caseNumber = DocumentTags.judgmentCaseNumber(doc.tags)
                                                val year = DocumentTags.judgmentYear(doc.tags)
                                                if (judgmentId != null) {
                                                    onDownloadReportable(judgmentId, caseNumber, year)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.document_reportable_unavailable),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onViewReportable = { reportableItem ->
                                                selectedDocument = reportableItem
                                                pendingAction = FileAction.OPEN
                                                viewModel.prepareFileForViewing(reportableItem)
                                            }
                                        )
                                    }
                                }

                                if (others.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(id = R.string.document_files_section),
                                            style = VakilTheme.typography.titleMedium,
                                            color = VakilTheme.colors.textPrimary
                                        )
                                    }
                                    items(others, key = { it.documentId }) { doc ->
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
                            } else {
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
    }

    if (showActionsSheet && selectedDocument != null) {
        val isJudgment = DocumentTags.isJudgment(selectedDocument!!.tags)
        val isReportable = DocumentTags.isReportable(selectedDocument!!.tags)
        ModalBottomSheet(
            onDismissRequest = { showActionsSheet = false },
            containerColor = VakilTheme.colors.bgSecondary
        ) {
            Column(modifier = Modifier.padding(bottom = VakilTheme.spacing.xl)) {
                ActionItem("Open", Icons.Default.FileOpen) {
                    val doc = selectedDocument!!
                    pendingAction = FileAction.OPEN
                    viewModel.prepareFileForViewing(doc)
                    showActionsSheet = false
                }
                ActionItem("Share", Icons.Default.Send) {
                    val doc = selectedDocument!!
                    pendingAction = FileAction.SHARE
                    viewModel.prepareFileForViewing(doc)
                    showActionsSheet = false
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = VakilTheme.colors.bgSurfaceSoft)
                ActionItem("Rename", Icons.Default.Edit) {
                    renameText = selectedDocument!!.fileName
                    showRenameDialog = true
                    showActionsSheet = false
                }
                if (!isJudgment || isReportable) {
                    ActionItem("Move to Case", Icons.Default.DriveFileMove) {
                        selectedMoveCaseId = selectedDocument!!.caseId
                        showMoveDialog = true
                        showActionsSheet = false
                    }
                }
                ActionItem("Delete", Icons.Default.Delete, isError = true) {
                    viewModel.deleteDocument(selectedDocument!!)
                    showActionsSheet = false
                }
            }
        }
    }

    if (showRenameDialog && selectedDocument != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = VakilTheme.colors.bgElevated,
            title = { Text(text = "Rename Document", style = VakilTheme.typography.headlineMedium) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New file name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VakilTheme.colors.accentPrimary,
                        unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameDocument(selectedDocument!!, renameText.trim())
                    showRenameDialog = false
                }) {
                    ButtonLabel(
                        text = "Save",
                        style = VakilTheme.typography.labelMedium.copy(color = VakilTheme.colors.accentPrimary)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    ButtonLabel(
                        text = "Cancel",
                        style = VakilTheme.typography.labelMedium.copy(color = VakilTheme.colors.textSecondary)
                    )
                }
            }
        )
    }

    if (showMoveDialog && selectedDocument != null) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            containerColor = VakilTheme.colors.bgElevated,
            title = { Text(text = "Move to Case", style = VakilTheme.typography.headlineMedium) },
            text = {
                when (casesState) {
                    CasePickerUiState.Loading -> CircularProgressIndicator(color = VakilTheme.colors.accentPrimary)
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
                }) {
                    ButtonLabel(
                        text = "Move",
                        style = VakilTheme.typography.labelMedium.copy(color = VakilTheme.colors.accentPrimary)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) {
                    ButtonLabel(
                        text = "Cancel",
                        style = VakilTheme.typography.labelMedium.copy(color = VakilTheme.colors.textSecondary)
                    )
                }
            }
        )
    }
}

@Composable
private fun ActionItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isError: Boolean = false, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text = label, style = VakilTheme.typography.bodyLarge, color = if (isError) VakilTheme.colors.error else VakilTheme.colors.textPrimary) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null, tint = if (isError) VakilTheme.colors.error else VakilTheme.colors.accentPrimary) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(
    document: Document,
    previewPath: String?,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VakilTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VakilTheme.colors.bgSurfaceSoft),
                contentAlignment = Alignment.Center
            ) {
                if (document.fileType.startsWith("image")) {
                    AsyncImage(
                        model = previewPath ?: document.thumbnailPath ?: document.filePath,
                        contentDescription = document.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = VakilTheme.colors.accentPrimary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = VakilTheme.typography.bodyLarge,
                    color = VakilTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatMeta(document.fileSizeBytes, document.createdAt),
                    style = VakilTheme.typography.labelSmall,
                    color = VakilTheme.colors.textSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JudgmentDocumentRow(
    document: Document,
    previewPath: String?,
    reportableDocument: Document?,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onDownloadReportable: () -> Unit,
    onViewReportable: (Document) -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
    ) {
        Column(
            modifier = Modifier.padding(VakilTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VakilTheme.colors.bgSurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (document.fileType.startsWith("image")) {
                        AsyncImage(
                            model = previewPath ?: document.thumbnailPath ?: document.filePath,
                            contentDescription = document.fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = VakilTheme.colors.accentPrimary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.fileName,
                        style = VakilTheme.typography.bodyLarge,
                        color = VakilTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatMeta(document.fileSizeBytes, document.createdAt),
                        style = VakilTheme.typography.labelSmall,
                        color = VakilTheme.colors.textSecondary
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    if (reportableDocument == null) {
                        onDownloadReportable()
                    } else {
                        onViewReportable(reportableDocument)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                ButtonLabel(
                    text = if (reportableDocument == null) {
                        stringResource(id = R.string.document_download_reportable)
                    } else {
                        stringResource(id = R.string.document_view_reportable)
                    },
                    style = VakilTheme.typography.labelMedium
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
            value = selected?.let { "${it.caseName} • ${it.caseNumber}" } ?: "No Case Associated",
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Select Target Case") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VakilTheme.colors.accentPrimary,
                unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft
            ),
            textStyle = VakilTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(VakilTheme.colors.bgElevated)
        ) {
            DropdownMenuItem(
                text = { Text(text = "No Case", color = VakilTheme.colors.textPrimary) },
                onClick = {
                    onSelectedCaseId(null)
                    expanded = false
                }
            )
            cases.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = "${item.caseName} • ${item.caseNumber}", color = VakilTheme.colors.textPrimary) },
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

private fun getFileInfo(context: Context, uri: Uri): FileInfo? {
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
    if (size == null || size <= 0) return null
    return FileInfo(sizeBytes = size, mimeType = mimeType)
}

private data class FileInfo(val sizeBytes: Long, val mimeType: String)

private const val LARGE_FILE_THRESHOLD = 20L * 1024 * 1024
private const val VIDEO_WARN_THRESHOLD = 100L * 1024 * 1024

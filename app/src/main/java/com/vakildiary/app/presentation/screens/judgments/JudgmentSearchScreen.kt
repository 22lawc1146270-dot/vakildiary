package com.vakildiary.app.presentation.screens.judgments

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.R
import com.vakildiary.app.core.Result
import com.vakildiary.app.core.ShareUtils
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.presentation.viewmodels.JudgmentDownloadUiState
import com.vakildiary.app.presentation.viewmodels.JudgmentSyncState
import com.vakildiary.app.presentation.viewmodels.JudgmentSearchUiState
import com.vakildiary.app.presentation.viewmodels.JudgmentSearchViewModel
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import com.vakildiary.app.presentation.util.rememberIsOnline
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JudgmentSearchScreen(
    onBack: () -> Unit,
    viewModel: JudgmentSearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(Year.now().value.toString()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedCaseId by remember { mutableStateOf<String?>(null) }
    var previewItem by remember { mutableStateOf<JudgmentSearchResult?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    val years = remember {
        val currentYear = Year.now().value
        (currentYear downTo 1950).map { it.toString() }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val casesState by viewModel.casesState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isOnline by rememberIsOnline()
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<JudgmentFileAction?>(null) }
    var pendingDocument by remember { mutableStateOf<Document?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fileEvents.collect { result ->
            when (result) {
                is Result.Success -> {
                    val action = pendingAction
                    val document = pendingDocument
                    if (action != null && document != null) {
                        when (action) {
                            JudgmentFileAction.OPEN -> ShareUtils.openFile(context, result.data, document.fileType)
                            JudgmentFileAction.SHARE -> ShareUtils.shareFile(context, result.data, document.fileType)
                        }
                    }
                    pendingAction = null
                    pendingDocument = null
                }
                is Result.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.judgment_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(text = stringResource(id = R.string.judgment_search_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = !yearExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(id = R.string.judgment_year)) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        years.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    year = option
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (!isOnline) {
                item {
                    Text(
                        text = stringResource(id = R.string.judgment_offline),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                when (casesState) {
                    CasePickerUiState.Loading -> CircularProgressIndicator()
                    is CasePickerUiState.Error -> Text(text = (casesState as CasePickerUiState.Error).message)
                    is CasePickerUiState.Success -> {
                        val cases = (casesState as CasePickerUiState.Success).cases
                        CaseDropdown(
                            cases = cases,
                            selectedCaseId = selectedCaseId,
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            onSelected = { selectedCaseId = it }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.search(query, year) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isOnline && year.isNotBlank()
                ) {
                    Text(text = stringResource(id = R.string.judgment_search_button))
                }
            }

            item {
                if (syncState is JudgmentSyncState.Syncing) {
                    Text(
                        text = (syncState as JudgmentSyncState.Syncing).message,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                when (val state = downloadState) {
                    JudgmentDownloadUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = stringResource(id = R.string.judgment_downloading),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is JudgmentDownloadUiState.Error -> Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    is JudgmentDownloadUiState.Success -> {
                        if (state.document != null) {
                            Text(
                                text = stringResource(id = R.string.judgment_download_success_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        val document = state.document
                        if (document != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        pendingAction = JudgmentFileAction.OPEN
                                        pendingDocument = document
                                        viewModel.prepareFileForViewing(document)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = stringResource(id = R.string.judgment_open_button))
                                }
                                Button(
                                    onClick = {
                                        pendingAction = JudgmentFileAction.SHARE
                                        pendingDocument = document
                                        viewModel.prepareFileForViewing(document)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = stringResource(id = R.string.judgment_share_button))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            when (val state = uiState) {
                JudgmentSearchUiState.Loading -> {
                    item { CircularProgressIndicator() }
                }
                is JudgmentSearchUiState.Error -> {
                    item {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is JudgmentSearchUiState.Success -> {
                    if (state.results.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.judgment_results),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    items(state.results) { item ->
                        JudgmentRow(
                            item = item,
                            canDownload = isOnline,
                            onDownload = { viewModel.download(item, selectedCaseId) },
                            onPreview = {
                                previewItem = item
                                showPreview = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPreview && previewItem != null) {
        ModalBottomSheet(onDismissRequest = { showPreview = false }) {
            JudgmentPreview(
                item = previewItem!!,
                canDownload = isOnline,
                onDownload = { viewModel.download(previewItem!!, selectedCaseId) }
            )
        }
    }
}

@Composable
private fun JudgmentRow(
    item: JudgmentSearchResult,
    canDownload: Boolean,
    onDownload: () -> Unit,
    onPreview: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val heading = remember(item) {
                val p = item.petitioner?.trim().orEmpty()
                val r = item.respondent?.trim().orEmpty()
                val c = item.caseNumber?.trim().orEmpty()
                
                buildString {
                    if (p.isNotBlank() && r.isNotBlank()) {
                        append(p)
                        append(" v. ")
                        append(r)
                    } else if (p.isNotBlank()) {
                        append(p)
                    } else if (r.isNotBlank()) {
                        append(r)
                    }
                    
                    if (c.isNotBlank()) {
                        if (isNotEmpty()) append(" ")
                        append(c)
                    }
                    
                    if (isEmpty()) {
                        append(item.title)
                    }
                }
            }
            
            Text(
                text = heading, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (!item.coram.isNullOrBlank()) {
                Text(
                    text = "${stringResource(id = R.string.judgment_coram)}: ${item.coram}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            item.dateOfJudgment?.let { date ->
                Text(
                    text = "${stringResource(id = R.string.judgment_date)}: ${formatDate(date)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (!item.citation.isNullOrBlank() || !item.bench.isNullOrBlank()) {
                val meta = listOfNotNull(
                    item.citation?.takeIf { it.isNotBlank() },
                    item.bench?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(text = meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.judgment_preview_button))
                }
                Button(
                    onClick = onDownload,
                    modifier = Modifier.weight(1f),
                    enabled = canDownload
                ) {
                    Text(text = stringResource(id = R.string.judgment_download_button))
                }
            }
        }
    }
}

@Composable
private fun JudgmentPreview(
    item: JudgmentSearchResult,
    canDownload: Boolean,
    onDownload: () -> Unit
) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val heading = remember(item) {
                val p = item.petitioner?.trim().orEmpty()
                val r = item.respondent?.trim().orEmpty()
                val c = item.caseNumber?.trim().orEmpty()
                
                buildString {
                    if (p.isNotBlank() && r.isNotBlank()) {
                        append(p)
                        append(" v. ")
                        append(r)
                    } else if (p.isNotBlank()) {
                        append(p)
                    } else if (r.isNotBlank()) {
                        append(r)
                    }
                    
                    if (c.isNotBlank()) {
                        if (isNotEmpty()) append(" ")
                        append(c)
                    }
                    
                    if (isEmpty()) {
                        append(item.title)
                    }
                }
            }
            
            Text(
                text = heading, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (!item.coram.isNullOrBlank()) {
                Text(text = "${stringResource(id = R.string.judgment_coram)}: ${item.coram}")
            }
            item.dateOfJudgment?.let { date ->
                Text(text = "${stringResource(id = R.string.judgment_date)}: ${formatDate(date)}")
            }
            if (!item.citation.isNullOrBlank()) {
                Text(text = "${stringResource(id = R.string.judgment_citation)}: ${item.citation}")
            }
            if (!item.bench.isNullOrBlank()) {
                Text(text = "${stringResource(id = R.string.judgment_bench)}: ${item.bench}")
            }
        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            enabled = canDownload
        ) {
            Text(text = stringResource(id = R.string.judgment_download_button))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseDropdown(
    cases: List<Case>,
    selectedCaseId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (String?) -> Unit
) {
    val selected = cases.firstOrNull { it.caseId == selectedCaseId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.caseName} • ${it.caseNumber}" }
                ?: stringResource(id = R.string.judgment_no_case),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(id = R.string.judgment_attach_case)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.judgment_no_case_option)) },
                onClick = { onSelected(null); onExpandedChange(false) }
            )
            cases.forEach { caseItem ->
                DropdownMenuItem(
                    text = { Text(text = "${caseItem.caseName} • ${caseItem.caseNumber}") },
                    onClick = {
                        onSelected(caseItem.caseId)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val date = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

private enum class JudgmentFileAction { OPEN, SHARE }

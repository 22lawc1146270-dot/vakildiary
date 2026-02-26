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
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.presentation.viewmodels.JudgmentDownloadUiState
import com.vakildiary.app.presentation.viewmodels.JudgmentSyncState
import com.vakildiary.app.presentation.viewmodels.JudgmentSearchUiState
import com.vakildiary.app.presentation.viewmodels.JudgmentSearchViewModel
import com.vakildiary.app.presentation.components.ButtonLabel
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
    var previewItem by remember { mutableStateOf<JudgmentSearchResult?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val years = remember {
        val currentYear = Year.now().value
        (currentYear downTo 1950).map { it.toString() }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
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
                Button(
                    onClick = {
                        hasSearched = true
                        viewModel.search(query, year)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isOnline && year.isNotBlank()
                ) {
                    ButtonLabel(text = stringResource(id = R.string.judgment_search_button))
                }
            }

            if (hasSearched) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(text = stringResource(id = R.string.judgment_search_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                    ButtonLabel(text = stringResource(id = R.string.judgment_open_button))
                                }
                                Button(
                                    onClick = {
                                        pendingAction = JudgmentFileAction.SHARE
                                        pendingDocument = document
                                        viewModel.prepareFileForViewing(document)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ButtonLabel(text = stringResource(id = R.string.judgment_share_button))
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
                            onDownload = { viewModel.download(item, year) },
                            onPreview = {
                                previewItem = item
                                showPreview = true
                            },
                            selectedYear = year
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
                onDownload = { viewModel.download(previewItem!!, year) },
                selectedYear = year
            )
        }
    }
}

@Composable
private fun JudgmentRow(
    item: JudgmentSearchResult,
    canDownload: Boolean,
    onDownload: () -> Unit,
    onPreview: () -> Unit,
    selectedYear: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val heading = remember(item, selectedYear) {
                val p = item.petitioner?.trim().orEmpty()
                val r = item.respondent?.trim().orEmpty()
                val yearLabel = item.dateOfJudgment?.let { extractYear(it) } ?: selectedYear
                val citation = item.citation?.trim().orEmpty()
                
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
                    
                    if (isEmpty()) {
                        append(item.title)
                    }

                    if (yearLabel.isNotBlank()) {
                        append(" (")
                        append(yearLabel)
                        append(")")
                    }

                    if (citation.isNotBlank()) {
                        append(" ")
                        append(citation)
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

            if (!item.bench.isNullOrBlank()) {
                Text(
                    text = "${stringResource(id = R.string.judgment_bench)}: ${item.bench}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!item.caseNumber.isNullOrBlank()) {
                Text(
                    text = "${stringResource(id = R.string.judgment_case_number)}: ${item.caseNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f)
                ) {
                    ButtonLabel(text = stringResource(id = R.string.judgment_preview_button))
                }
                Button(
                    onClick = onDownload,
                    modifier = Modifier.weight(1f),
                    enabled = canDownload
                ) {
                    ButtonLabel(text = stringResource(id = R.string.judgment_download_button))
                }
            }
        }
    }
}

@Composable
private fun JudgmentPreview(
    item: JudgmentSearchResult,
    canDownload: Boolean,
    onDownload: () -> Unit,
    selectedYear: String
) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val heading = remember(item, selectedYear) {
                val p = item.petitioner?.trim().orEmpty()
                val r = item.respondent?.trim().orEmpty()
                val yearLabel = item.dateOfJudgment?.let { extractYear(it) } ?: selectedYear
                val citation = item.citation?.trim().orEmpty()
                
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
                    
                    if (isEmpty()) {
                        append(item.title)
                    }

                    if (yearLabel.isNotBlank()) {
                        append(" (")
                        append(yearLabel)
                        append(")")
                    }

                    if (citation.isNotBlank()) {
                        append(" ")
                        append(citation)
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
            if (!item.bench.isNullOrBlank()) {
                Text(text = "${stringResource(id = R.string.judgment_bench)}: ${item.bench}")
            }
            if (!item.caseNumber.isNullOrBlank()) {
                Text(text = "${stringResource(id = R.string.judgment_case_number)}: ${item.caseNumber}")
            }
        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            enabled = canDownload
        ) {
            ButtonLabel(text = stringResource(id = R.string.judgment_download_button))
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val date = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

private fun extractYear(epochMillis: Long): String {
    return java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .year
        .toString()
}

private enum class JudgmentFileAction { OPEN, SHARE }

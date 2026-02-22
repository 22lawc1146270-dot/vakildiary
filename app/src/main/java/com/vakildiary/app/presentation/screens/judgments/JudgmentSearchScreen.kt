package com.vakildiary.app.presentation.screens.judgments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.presentation.viewmodels.JudgmentDownloadUiState
import com.vakildiary.app.presentation.viewmodels.JudgmentSearchUiState
import com.vakildiary.app.presentation.viewmodels.JudgmentSearchViewModel
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import com.vakildiary.app.presentation.util.rememberIsOnline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JudgmentSearchScreen(
    onBack: () -> Unit,
    viewModel: JudgmentSearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCaseId by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val casesState by viewModel.casesState.collectAsStateWithLifecycle()
    val isOnline by rememberIsOnline()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Judgment Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(text = "Search by case name, citation, bench, keyword") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text(text = "Year") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (!isOnline) {
                Text(
                    text = "No internet connection. Judgment search unavailable.",
                    color = MaterialTheme.colorScheme.error
                )
            }

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

            Button(
                onClick = { viewModel.search(query, year) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isOnline
            ) {
                Text(text = "Search")
            }

            when (downloadState) {
                JudgmentDownloadUiState.Loading -> CircularProgressIndicator()
                is JudgmentDownloadUiState.Error -> Text(
                    text = (downloadState as JudgmentDownloadUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                is JudgmentDownloadUiState.Success -> {
                    val message = (downloadState as JudgmentDownloadUiState.Success).message
                    if (message.isNotBlank()) {
                        Text(text = message, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                JudgmentSearchUiState.Loading -> CircularProgressIndicator()
                is JudgmentSearchUiState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
                is JudgmentSearchUiState.Success -> {
                    if (state.results.isNotEmpty()) {
                        Text(text = "Results", style = MaterialTheme.typography.titleMedium)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.results) { item ->
                            JudgmentRow(
                                item = item,
                                canDownload = isOnline,
                                onDownload = { viewModel.download(item, selectedCaseId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JudgmentRow(
    item: JudgmentSearchResult,
    canDownload: Boolean,
    onDownload: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth(),
                enabled = canDownload
            ) {
                Text(text = "Download Judgment")
            }
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
            value = selected?.let { "${it.caseName} • ${it.caseNumber}" } ?: "No case (store in Documents)",
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Attach to Case") },
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
                text = { Text(text = "No case") },
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

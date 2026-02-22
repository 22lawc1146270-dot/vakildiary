package com.vakildiary.app.presentation.screens.cases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.presentation.viewmodels.CaseListViewModel
import com.vakildiary.app.presentation.viewmodels.state.CaseListUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(
    onAddCase: () -> Unit = {},
    onOpenCase: (String) -> Unit = {},
    viewModel: CaseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(CaseSortOption.NEXT_HEARING) }
    var courtFilters by remember { mutableStateOf(setOf<CourtType>()) }
    var typeFilters by remember { mutableStateOf(setOf<CaseType>()) }
    var stageFilters by remember { mutableStateOf(setOf<CaseStage>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Cases") },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { /* TODO: open search */ }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCase) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Case")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                CaseListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CaseListUiState.Success -> {
                    val filtered = state.cases
                        .filter { case ->
                            (courtFilters.isEmpty() || courtFilters.contains(case.courtType)) &&
                                (typeFilters.isEmpty() || typeFilters.contains(case.caseType)) &&
                                (stageFilters.isEmpty() || stageFilters.contains(case.caseStage))
                        }
                        .sortedWith(
                            when (sortOption) {
                                CaseSortOption.NEXT_HEARING -> compareBy { it.nextHearingDate ?: Long.MAX_VALUE }
                                CaseSortOption.CASE_NAME -> compareBy { it.caseName.lowercase() }
                                CaseSortOption.RECENTLY_ADDED -> compareByDescending { it.createdAt }
                                CaseSortOption.COURT -> compareBy { it.courtName.lowercase() }
                            }
                        )

                    if (filtered.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "No cases yet", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = onAddCase) {
                                Text(text = "Add Case")
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = viewModel::onSearchQueryChanged,
                                    label = { Text(text = "Search cases") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Sort", style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CaseSortOption.values().forEach { option ->
                                        FilterChip(
                                            selected = sortOption == option,
                                            onClick = { sortOption = option },
                                            label = { Text(text = option.label) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            items(filtered, key = { it.caseId }) { caseItem ->
                                CaseCard(caseItem, onClick = { onOpenCase(caseItem.caseId) })
                            }
                        }
                    }
                }
                is CaseListUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.onSearchQueryChanged(searchQuery) }) {
                            Text(text = "Retry")
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Filter by Court Type", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourtType.values().forEach { ct ->
                        FilterChip(
                            selected = courtFilters.contains(ct),
                            onClick = {
                                courtFilters = courtFilters.toggle(ct)
                            },
                            label = { Text(text = ct.name) }
                        )
                    }
                }
                Text(text = "Filter by Case Type", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaseType.values().forEach { ct ->
                        FilterChip(
                            selected = typeFilters.contains(ct),
                            onClick = { typeFilters = typeFilters.toggle(ct) },
                            label = { Text(text = ct.name) }
                        )
                    }
                }
                Text(text = "Filter by Stage", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaseStage.values().forEach { cs ->
                        FilterChip(
                            selected = stageFilters.contains(cs),
                            onClick = { stageFilters = stageFilters.toggle(cs) },
                            label = { Text(text = cs.name) }
                        )
                    }
                }
                Button(
                    onClick = { showFilters = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Apply Filters")
                }
            }
        }
    }
}

private enum class CaseSortOption(val label: String) {
    NEXT_HEARING("Next Hearing"),
    CASE_NAME("Case Name A-Z"),
    RECENTLY_ADDED("Recently Added"),
    COURT("Court")
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (contains(value)) this - value else this + value
}

@Composable
fun CaseCard(
    case: Case,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = case.caseName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Case No: ${case.caseNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Court: ${case.courtName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Next hearing: ${formatHearingDate(case.nextHearingDate)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatHearingDate(epochMillis: Long?): String {
    if (epochMillis == null) return "--"
    val date = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

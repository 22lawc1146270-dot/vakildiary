package com.vakildiary.app.presentation.screens.cases

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.domain.model.displayLabel
import com.vakildiary.app.presentation.components.AppCard
import com.vakildiary.app.presentation.components.UrgencyBadge
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.viewmodels.CaseListViewModel
import com.vakildiary.app.presentation.viewmodels.state.CaseListUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
                title = { Text(text = "Cases", style = VakilTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter", tint = VakilTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VakilTheme.colors.bgPrimary,
                    titleContentColor = VakilTheme.colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "FABScale")
            
            FloatingActionButton(
                onClick = onAddCase,
                shape = CircleShape,
                containerColor = VakilTheme.colors.accentPrimary,
                contentColor = VakilTheme.colors.onAccent,
                interactionSource = interactionSource,
                modifier = Modifier.scale(scale),
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Case")
            }
        },
        containerColor = VakilTheme.colors.bgPrimary
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                CaseListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VakilTheme.colors.accentPrimary
                    )
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

                    Column {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::onSearchQueryChanged,
                            onSortOptionChange = { sortOption = it },
                            currentSortOption = sortOption
                        )
                        
                        if (filtered.isEmpty()) {
                            NoCasesPlaceholder(onAddCase)
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(VakilTheme.spacing.md),
                                verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)
                            ) {
                                items(filtered, key = { it.caseId }) { caseItem ->
                                    CaseCard(caseItem, onClick = { onOpenCase(caseItem.caseId) })
                                }
                            }
                        }
                    }
                }
                is CaseListUiState.Error -> {
                    ErrorPlaceholder(state.message) { viewModel.onSearchQueryChanged(searchQuery) }
                }
            }
        }
    }

    if (showFilters) {
        FilterBottomSheet(
            courtFilters = courtFilters,
            typeFilters = typeFilters,
            stageFilters = stageFilters,
            onCourtToggle = { courtFilters = courtFilters.toggle(it) },
            onTypeToggle = { typeFilters = typeFilters.toggle(it) },
            onStageToggle = { stageFilters = stageFilters.toggle(it) },
            onDismiss = { showFilters = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSortOptionChange: (CaseSortOption) -> Unit,
    currentSortOption: CaseSortOption
) {
    Column(modifier = Modifier.padding(horizontal = VakilTheme.spacing.md)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search case name, client...", style = VakilTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VakilTheme.colors.textTertiary) },
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VakilTheme.colors.accentPrimary,
                unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft,
                focusedContainerColor = VakilTheme.colors.bgSecondary,
                unfocusedContainerColor = VakilTheme.colors.bgSecondary,
                unfocusedTextColor = VakilTheme.colors.textPrimary,
                focusedTextColor = VakilTheme.colors.textPrimary
            ),
            textStyle = VakilTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(VakilTheme.spacing.sm))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)
        ) {
            CaseSortOption.values().forEach { option ->
                val selected = currentSortOption == option
                FilterChip(
                    selected = selected,
                    onClick = { onSortOptionChange(option) },
                    label = { Text(text = option.label, style = VakilTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VakilTheme.colors.accentPrimary,
                        selectedLabelColor = VakilTheme.colors.onAccent,
                        containerColor = VakilTheme.colors.bgElevated,
                        labelColor = VakilTheme.colors.textSecondary
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
private fun CaseCard(
    case: Case,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(VakilTheme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = case.caseName, 
                        style = VakilTheme.typography.headlineMedium,
                        color = VakilTheme.colors.textPrimary
                    )
                    val clientLabel = case.clientName.takeIf { it.isNotBlank() }
                        ?.let { "Client: $it" }
                        ?: "Client: Not set"
                    Text(
                        text = clientLabel,
                        style = VakilTheme.typography.labelMedium,
                        color = VakilTheme.colors.accentPrimary
                    )
                }
                
                case.nextHearingDate?.let { epoch ->
                    val days = getDaysRemaining(epoch)
                    UrgencyBadge(daysRemaining = days.toInt())
                }
            }
            
            Spacer(modifier = Modifier.height(VakilTheme.spacing.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Court: ${case.courtName}", 
                        style = VakilTheme.typography.bodyMedium,
                        color = VakilTheme.colors.textSecondary
                    )
                    Text(
                        text = "No: ${case.caseNumber}", 
                        style = VakilTheme.typography.labelSmall,
                        color = VakilTheme.colors.textTertiary
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = VakilTheme.colors.bgSurfaceSoft
                ) {
                    Text(
                        text = case.caseStage.displayLabel(case.customStage),
                        style = VakilTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = VakilTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    courtFilters: Set<CourtType>,
    typeFilters: Set<CaseType>,
    stageFilters: Set<CaseStage>,
    onCourtToggle: (CourtType) -> Unit,
    onTypeToggle: (CaseType) -> Unit,
    onStageToggle: (CaseStage) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VakilTheme.colors.bgSecondary,
        contentColor = VakilTheme.colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .padding(VakilTheme.spacing.md)
                .padding(bottom = VakilTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)
        ) {
            Text(text = "Filter Cases", style = VakilTheme.typography.headlineMedium)
            
            FilterGroup("Court Type", CourtType.values(), courtFilters, onCourtToggle) { it.displayLabel() }
            FilterGroup("Case Type", CaseType.values(), typeFilters, onTypeToggle) { it.displayLabel() }
            FilterGroup("Stage", CaseStage.values(), stageFilters, onStageToggle) { it.displayLabel() }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VakilTheme.colors.accentPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Filters", style = VakilTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterGroup(
    title: String,
    options: Array<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    labelFor: (T) -> String = { it.toString().replace("_", " ") }
) {
    Column {
        Text(text = title, style = VakilTheme.typography.labelMedium, color = VakilTheme.colors.textSecondary)
        Spacer(modifier = Modifier.height(VakilTheme.spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)
        ) {
            options.forEach { opt ->
                val isSelected = selected.contains(opt)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(opt) },
                    label = { Text(text = labelFor(opt), style = VakilTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VakilTheme.colors.accentPrimary,
                        containerColor = VakilTheme.colors.bgElevated,
                        labelColor = VakilTheme.colors.textSecondary,
                        selectedLabelColor = VakilTheme.colors.onAccent
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
private fun NoCasesPlaceholder(onAddCase: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "No cases found", style = VakilTheme.typography.headlineMedium, color = VakilTheme.colors.textTertiary)
            Spacer(modifier = Modifier.height(VakilTheme.spacing.md))
            Button(
                onClick = onAddCase,
                colors = ButtonDefaults.buttonColors(containerColor = VakilTheme.colors.accentPrimary)
            ) {
                Text("Add Your First Case")
            }
        }
    }
}

@Composable
private fun ErrorPlaceholder(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(VakilTheme.spacing.lg)) {
            Text(text = message, color = VakilTheme.colors.error, style = VakilTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(VakilTheme.spacing.md))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

private enum class CaseSortOption(val label: String) {
    NEXT_HEARING("Hearing"),
    CASE_NAME("Name"),
    RECENTLY_ADDED("Newest"),
    COURT("Court")
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (contains(value)) this - value else this + value
}

private fun getDaysRemaining(epochMillis: Long): Long {
    val target = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val now = LocalDate.now()
    return ChronoUnit.DAYS.between(now, target)
}

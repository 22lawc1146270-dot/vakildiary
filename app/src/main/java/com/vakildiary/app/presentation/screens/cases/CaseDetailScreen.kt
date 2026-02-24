package com.vakildiary.app.presentation.screens.cases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.presentation.viewmodels.CaseDetailViewModel
import com.vakildiary.app.presentation.viewmodels.CaseExportViewModel
import com.vakildiary.app.presentation.viewmodels.state.CaseDetailUiState
import com.vakildiary.app.presentation.screens.tasks.TaskListScreen
import com.vakildiary.app.presentation.screens.documents.DocumentListScreen
import com.vakildiary.app.presentation.screens.meetings.MeetingListScreen
import com.vakildiary.app.presentation.viewmodels.FeesViewModel
import com.vakildiary.app.presentation.viewmodels.state.FeeExportUiState
import com.vakildiary.app.core.ShareUtils
import com.vakildiary.app.presentation.viewmodels.state.CaseExportUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.displayLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    caseId: String,
    onAddTask: (String) -> Unit = {},
    onAddHearing: () -> Unit = {},
    onAddPayment: () -> Unit = {},
    onAddDocument: (String) -> Unit = {},
    onEdit: (String) -> Unit = {},
    onAddMeeting: (String) -> Unit = {},
    viewModel: CaseDetailViewModel = hiltViewModel()
) {
    val tabs = listOf("Overview", "History", "Tasks", "Fees", "Documents", "Meetings")
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState(caseId).collectAsStateWithLifecycle()
    val advocateName by viewModel.advocateName.collectAsStateWithLifecycle()
    val exportViewModel: CaseExportViewModel = hiltViewModel()
    val summaryState by exportViewModel.summaryState.collectAsStateWithLifecycle()
    val historyState by exportViewModel.historyState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(summaryState) {
        val state = summaryState
        if (state is CaseExportUiState.Success && state.file != null) {
            ShareUtils.shareFile(context, state.file, "application/pdf")
            exportViewModel.resetSummaryState()
        }
    }

    LaunchedEffect(historyState) {
        val state = historyState
        if (state is CaseExportUiState.Success && state.file != null) {
            ShareUtils.shareFile(context, state.file, "application/pdf")
            exportViewModel.resetHistoryState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Case Detail #$caseId") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title) }
                    )
                }
            }

            when (val state = uiState) {
                CaseDetailUiState.Loading -> {
                    Text(text = "Loading...", modifier = Modifier.padding(16.dp))
                }
                is CaseDetailUiState.Error -> {
                    Text(text = state.message, modifier = Modifier.padding(16.dp))
                }
                is CaseDetailUiState.Success -> {
                    when (selectedTab) {
                        0 -> OverviewTab(
                            caseName = state.case.caseName,
                            caseNumber = state.case.caseNumber,
                            courtName = state.case.courtName,
                            clientName = state.case.clientName,
                            nextHearingDate = state.case.nextHearingDate,
                            caseStage = state.case.caseStage,
                            customStage = state.case.customStage,
                            agreedFees = state.case.totalAgreedFees ?: 0.0,
                            totalReceived = state.payments.sumOf { it.amount },
                            advocateName = advocateName ?: "Counsel",
                            onAddHearing = onAddHearing,
                            onAddTask = { onAddTask(caseId) },
                            onAddPayment = onAddPayment,
                            onAddDocument = { onAddDocument(caseId) },
                            onEdit = { onEdit(caseId) },
                            summaryState = summaryState,
                            historyState = historyState,
                            onExportSummary = { exportViewModel.exportSummary(state.case) },
                            onExportHistory = { exportViewModel.exportHistory(state.case, state.hearings) },
                            onUpdateNextHearing = { newDate ->
                                scope.launch {
                                    viewModel.updateCase(state.case.copy(nextHearingDate = newDate))
                                }
                            },
                            onStageChanged = { newStage ->
                                scope.launch {
                                    val customStage = if (newStage == CaseStage.CUSTOM) state.case.customStage else null
                                    viewModel.updateCase(state.case.copy(caseStage = newStage, customStage = customStage))
                                }
                            },
                            onCustomStageChanged = { custom ->
                                scope.launch {
                                    viewModel.updateCase(
                                        state.case.copy(
                                            caseStage = CaseStage.CUSTOM,
                                            customStage = custom.trim().ifBlank { null }
                                        )
                                    )
                                }
                            },
                            onArchive = {
                                scope.launch {
                                    viewModel.archiveCase(state.case.caseId)
                                }
                            }
                        )
                        1 -> HistoryTab(
                            caseName = state.case.caseName,
                            clientName = state.case.clientName,
                            courtName = state.case.courtName,
                            advocateName = advocateName ?: "Counsel",
                            hearings = state.hearings
                        )
                        2 -> TasksTab(
                            caseId = caseId,
                            onAddTask = { onAddTask(caseId) }
                        )
                        3 -> FeesTab(
                            caseData = state.case,
                            payments = state.payments,
                            onAddPayment = onAddPayment
                        )
                        4 -> DocumentListScreen(caseId = caseId, showTopBar = false)
                        else -> MeetingsTab(
                            caseId = caseId,
                            onAddMeeting = { onAddMeeting(caseId) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewTab(
    caseName: String,
    caseNumber: String,
    courtName: String,
    clientName: String,
    nextHearingDate: Long?,
    caseStage: CaseStage,
    customStage: String?,
    agreedFees: Double,
    totalReceived: Double,
    advocateName: String,
    onAddHearing: () -> Unit,
    onAddTask: () -> Unit,
    onAddPayment: () -> Unit,
    onAddDocument: () -> Unit,
    onEdit: () -> Unit,
    summaryState: CaseExportUiState,
    historyState: CaseExportUiState,
    onExportSummary: () -> Unit,
    onExportHistory: () -> Unit,
    onUpdateNextHearing: (Long) -> Unit,
    onStageChanged: (CaseStage) -> Unit,
    onCustomStageChanged: (String) -> Unit,
    onArchive: () -> Unit
) {
    val outstanding = (agreedFees - totalReceived).coerceAtLeast(0.0)
    val progress = if (agreedFees > 0.0) {
        (totalReceived / agreedFees).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var nextDateError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = caseName, style = MaterialTheme.typography.titleLarge)
        Text(text = "Case No: $caseNumber")
        Text(text = "Court: $courtName")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Next Hearing: ${formatDate(nextHearingDate)}")
            Button(onClick = { showDatePicker = true }) {
                Text(text = "Edit")
            }
            if (nextHearingDate != null) {
                val context = LocalContext.current
                IconButton(onClick = {
                    ShareUtils.shareHearingDateText(
                        context = context,
                        clientName = clientName.takeIf { it.isNotBlank() } ?: "Client",
                        caseName = caseName,
                        date = formatDate(nextHearingDate),
                        court = courtName,
                        advocateName = advocateName
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Hearing Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (!nextDateError.isNullOrBlank()) {
            Text(text = nextDateError.orEmpty(), color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Stage:")
            CaseStageDropdown(
                selected = caseStage,
                customStage = customStage,
                onSelected = onStageChanged
            )
        }
        if (caseStage == CaseStage.CUSTOM) {
            OutlinedTextField(
                value = customStage.orEmpty(),
                onValueChange = onCustomStageChanged,
                label = { Text(text = "Custom Stage") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddHearing) { Text(text = "Add Hearing") }
            Button(onClick = onAddTask) { Text(text = "Add Task") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddPayment) { Text(text = "Add Payment") }
            Button(onClick = onAddDocument) { Text(text = "Add Document") }
            Button(onClick = onArchive) { Text(text = "Archive") }
            Button(onClick = onEdit) { Text(text = "Edit") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onExportSummary,
                enabled = summaryState !is CaseExportUiState.Loading
            ) {
                Text(text = if (summaryState is CaseExportUiState.Loading) "Exporting..." else "Export Summary")
            }
            Button(
                onClick = onExportHistory,
                enabled = historyState !is CaseExportUiState.Loading
            ) {
                Text(text = if (historyState is CaseExportUiState.Loading) "Exporting..." else "Export History")
            }
        }
        if (summaryState is CaseExportUiState.Error) {
            Text(text = summaryState.message, color = MaterialTheme.colorScheme.error)
        }
        if (historyState is CaseExportUiState.Error) {
            Text(text = historyState.message, color = MaterialTheme.colorScheme.error)
        }

        Text(text = "Fee Summary", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Agreed Fees")
            Text(text = "₹${agreedFees.toInt()}")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Total Received")
            Text(text = "₹${totalReceived.toInt()}")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Outstanding")
            Text(text = "₹${outstanding.toInt()}")
        }
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = nextHearingDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = pickerState.selectedDateMillis
                    if (selected != null && selected >= System.currentTimeMillis()) {
                        nextDateError = null
                        onUpdateNextHearing(selected)
                        showDatePicker = false
                    } else {
                        nextDateError = "Next hearing date must be in the future"
                    }
                }) { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(text = "Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseStageDropdown(
    selected: CaseStage,
    customStage: String?,
    onSelected: (CaseStage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.displayLabel(customStage),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val stages = listOf(CaseStage.UNKNOWN) +
                CaseStage.values().filter { it != CaseStage.UNKNOWN && it != CaseStage.CUSTOM } +
                CaseStage.CUSTOM
            stages.forEach { stage ->
                DropdownMenuItem(
                    text = { Text(text = stage.displayLabel()) },
                    onClick = {
                        onSelected(stage)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryTab(
    caseName: String,
    clientName: String,
    courtName: String,
    advocateName: String,
    hearings: List<HearingHistory>
) {
    if (hearings.isEmpty()) {
        Text(text = "No hearing history yet", modifier = Modifier.padding(16.dp))
        return
    }
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(hearings, key = { it.hearingId }) { hearing ->
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(text = formatDate(hearing.hearingDate), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        ShareUtils.shareHearingDateText(
                            context = context,
                            clientName = clientName.takeIf { it.isNotBlank() } ?: "Client",
                            caseName = caseName,
                            date = formatDate(hearing.hearingDate),
                            court = courtName,
                            advocateName = advocateName
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Hearing Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(text = hearing.purpose, style = MaterialTheme.typography.labelLarge)
                Text(text = hearing.outcome ?: "No outcome recorded", style = MaterialTheme.typography.bodyMedium)
                hearing.nextDateGiven?.let {
                    Text(text = "Next date: ${formatDate(it)}", style = MaterialTheme.typography.bodySmall)
                }
                hearing.orderDetails?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun TasksTab(
    caseId: String,
    onAddTask: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tasks", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onAddTask) {
                Text(text = "Add Task")
            }
        }
        TaskListScreen(caseId = caseId)
    }
}

@Composable
private fun FeesTab(
    caseData: com.vakildiary.app.domain.model.Case,
    payments: List<com.vakildiary.app.domain.model.Payment>,
    onAddPayment: () -> Unit
) {
    val totalReceived = payments.sumOf { it.amount }
    val agreedFees = caseData.totalAgreedFees ?: 0.0
    val outstanding = (agreedFees - totalReceived).coerceAtLeast(0.0)
    val context = LocalContext.current
    val feesViewModel: FeesViewModel = hiltViewModel()
    val exportState by feesViewModel.exportState.collectAsStateWithLifecycle()

    LaunchedEffect(exportState) {
        val state = exportState
        if (state is FeeExportUiState.Success && state.file != null) {
            ShareUtils.shareFile(context, state.file, "application/pdf")
            feesViewModel.resetExportState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Fee Summary", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Agreed")
            Text(text = "₹${agreedFees.toInt()}")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Received")
            Text(text = "₹${totalReceived.toInt()}")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Outstanding")
            Text(text = "₹${outstanding.toInt()}")
        }
        LinearProgressIndicator(
            progress = if (agreedFees > 0.0) (totalReceived / agreedFees).toFloat().coerceIn(0f, 1f) else 0f,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddPayment) { Text(text = "Add Payment") }
            Button(
                onClick = { feesViewModel.exportFeeLedger(caseData, payments) },
                enabled = exportState !is FeeExportUiState.Loading
            ) { Text(text = "Export Ledger") }
            Button(
                onClick = {
                    ShareUtils.shareFeeSummaryText(
                        context = context,
                        clientName = caseData.clientName,
                        caseName = caseData.caseName,
                        agreed = "₹${agreedFees.toInt()}",
                        received = "₹${totalReceived.toInt()}",
                        outstanding = "₹${outstanding.toInt()}"
                    )
                }
            ) { Text(text = "Share Summary") }
        }

        if (exportState is FeeExportUiState.Error) {
            Text(text = (exportState as FeeExportUiState.Error).message, color = MaterialTheme.colorScheme.error)
        }

        Text(text = "Payment History", style = MaterialTheme.typography.titleMedium)
        if (payments.isEmpty()) {
            Text(text = "No payments yet")
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payments.sortedByDescending { it.paymentDate }, key = { it.paymentId }) { payment ->
                    PaymentRow(payment = payment)
                }
            }
        }
    }
}

@Composable
private fun MeetingsTab(
    caseId: String,
    onAddMeeting: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Meetings", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onAddMeeting) {
                Text(text = "Add Meeting")
            }
        }
        MeetingListScreen(caseId = caseId)
    }
}

@Composable
private fun PaymentRow(payment: com.vakildiary.app.domain.model.Payment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = payment.paymentMode.name.replace('_', ' '), style = MaterialTheme.typography.titleSmall)
            Text(text = formatDate(payment.paymentDate), style = MaterialTheme.typography.bodySmall)
            if (!payment.referenceNumber.isNullOrBlank()) {
                Text(text = "Ref: ${payment.referenceNumber}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(text = "₹${payment.amount.toInt()}", style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatDate(epochMillis: Long?): String {
    if (epochMillis == null) return "--"
    val date = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

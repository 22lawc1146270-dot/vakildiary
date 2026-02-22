package com.vakildiary.app.presentation.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vakildiary.app.domain.model.TaskType
import com.vakildiary.app.presentation.viewmodels.AddTaskViewModel
import com.vakildiary.app.presentation.viewmodels.state.AddTaskUiState
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    initialCaseId: String?,
    onBack: () -> Unit,
    viewModel: AddTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val casesState by viewModel.casesState.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf(TaskType.FILE_PETITION) }
    var dateMillis by remember { mutableStateOf<Long?>(null) }
    var timeText by remember { mutableStateOf("10:00") }
    var reminderMinutes by remember { mutableStateOf("60") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedCaseId by remember { mutableStateOf(initialCaseId.orEmpty()) }
    var caseDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Task") },
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState is AddTaskUiState.Error) {
                Text(text = (uiState as AddTaskUiState.Error).message)
            }

            when (casesState) {
                CasePickerUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is CasePickerUiState.Error -> {
                    Text(text = (casesState as CasePickerUiState.Error).message)
                }
                is CasePickerUiState.Success -> {
                    val cases = (casesState as CasePickerUiState.Success).cases
                    CaseDropdown(
                        cases = cases,
                        selectedCaseId = selectedCaseId,
                        expanded = caseDropdownExpanded,
                        enabled = initialCaseId.isNullOrBlank(),
                        onExpandedChange = { caseDropdownExpanded = it },
                        onSelected = { selected ->
                            selectedCaseId = selected.caseId
                            caseDropdownExpanded = false
                        }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(text = "Task Title") },
                modifier = Modifier.fillMaxWidth()
            )

            TaskTypeDropdown(
                selected = taskType,
                onSelected = { taskType = it }
            )

            OutlinedTextField(
                value = dateMillis?.let { formatDate(it) }.orEmpty(),
                onValueChange = {},
                label = { Text(text = "Deadline") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(imageVector = Icons.Default.Event, contentDescription = "Pick date")
                    }
                }
            )

            OutlinedTextField(
                value = timeText,
                onValueChange = { timeText = it },
                label = { Text(text = "Time (HH:mm)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = reminderMinutes,
                onValueChange = { reminderMinutes = it },
                label = { Text(text = "Reminder (minutes before)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val date = dateMillis ?: return@Button
                    val deadline = combineDateAndTime(date, timeText)
                    val minutes = reminderMinutes.toIntOrNull() ?: 0
                    viewModel.saveTask(
                        caseId = selectedCaseId,
                        title = title,
                        taskType = taskType,
                        deadlineMillis = deadline,
                        reminderMinutesBefore = minutes
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
                enabled = selectedCaseId.isNotBlank() && title.isNotBlank() && dateMillis != null
            ) {
                Text(text = "Save Task")
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AddTaskUiState.Success && (uiState as AddTaskUiState.Success).isSaved) {
            onBack()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(text = "Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseDropdown(
    cases: List<Case>,
    selectedCaseId: String,
    expanded: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (Case) -> Unit
) {
    val selectedCase = cases.firstOrNull { it.caseId == selectedCaseId }
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCase?.let { "${it.caseName} • ${it.caseNumber}" }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Case") },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled
        )

        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            cases.forEach { item ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(text = "${item.caseName} • ${item.caseNumber}") },
                    onClick = { onSelected(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTypeDropdown(
    selected: TaskType,
    onSelected: (TaskType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = taskTypeLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Task Type") },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TaskType.values().forEach { type ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(text = taskTypeLabel(type)) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun taskTypeLabel(type: TaskType): String {
    return when (type) {
        TaskType.FILE_PETITION -> "File Petition"
        TaskType.COLLECT_PAPERS -> "Collect Papers from Court"
        TaskType.VIEW_ORDERSHEET -> "View Ordersheet"
        TaskType.PHOTOCOPY -> "Get Photocopies"
        TaskType.PAY_COURT_FEES -> "Pay Court Fees"
        TaskType.PREPARE_ARGUMENTS -> "Prepare Arguments"
        TaskType.MEETING -> "Meeting"
        TaskType.CUSTOM -> "Custom"
    }
}

private fun combineDateAndTime(dateMillis: Long, timeText: String): Long {
    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val parts = timeText.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 10
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val dateTime = date.atTime(LocalTime.of(hour, minute))
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun formatDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

package com.vakildiary.app.presentation.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.TaskType
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.viewmodels.AddTaskViewModel
import com.vakildiary.app.presentation.viewmodels.state.AddTaskUiState
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                title = { Text(text = "Create Task", style = VakilTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = VakilTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VakilTheme.colors.bgPrimary,
                    titleContentColor = VakilTheme.colors.textPrimary
                )
            )
        },
        containerColor = VakilTheme.colors.bgPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VakilTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.lg)
        ) {
            if (uiState is AddTaskUiState.Error) {
                Surface(
                    color = VakilTheme.colors.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as AddTaskUiState.Error).message,
                        color = VakilTheme.colors.error,
                        style = VakilTheme.typography.labelSmall,
                        modifier = Modifier.padding(VakilTheme.spacing.sm)
                    )
                }
            }

            FormSection("Association") {
                when (casesState) {
                    CasePickerUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VakilTheme.colors.accentPrimary)
                    is CasePickerUiState.Error -> Text(text = (casesState as CasePickerUiState.Error).message, color = VakilTheme.colors.error)
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
            }

            FormSection("Task Information") {
                AppTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Task Description*"
                )

                TaskTypeDropdown(
                    selected = taskType,
                    onSelected = { taskType = it }
                )
            }

            FormSection("Deadline & Reminder") {
                AppTextField(
                    value = dateMillis?.let { formatDate(it) }.orEmpty(),
                    onValueChange = {},
                    label = "Due Date*",
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(imageVector = Icons.Default.Event, contentDescription = "Pick date", tint = VakilTheme.colors.accentPrimary)
                        }
                    }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AppTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = "Time (HH:mm)*"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AppTextField(
                            value = reminderMinutes,
                            onValueChange = { reminderMinutes = it },
                            label = "Reminder (mins before)",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                enabled = selectedCaseId.isNotBlank() && title.isNotBlank() && dateMillis != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VakilTheme.colors.accentPrimary,
                    disabledContainerColor = VakilTheme.colors.bgSurfaceSoft
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(VakilTheme.spacing.md)
            ) {
                Text(
                    text = "Save Task",
                    style = VakilTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
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
                }) { Text(text = "Confirm", color = VakilTheme.colors.accentPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(text = "Cancel", color = VakilTheme.colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = VakilTheme.colors.bgElevated)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = VakilTheme.typography.labelSmall,
            color = VakilTheme.colors.accentPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = VakilTheme.spacing.sm)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, style = VakilTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VakilTheme.colors.accentPrimary,
            unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft,
            focusedTextColor = VakilTheme.colors.textPrimary,
            unfocusedTextColor = VakilTheme.colors.textPrimary,
            focusedLabelColor = VakilTheme.colors.accentPrimary,
            unfocusedLabelColor = VakilTheme.colors.textTertiary
        ),
        textStyle = VakilTheme.typography.bodyLarge
    )
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
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCase?.let { "${it.caseName} • ${it.caseNumber}" }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Select Case", style = VakilTheme.typography.bodyMedium) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VakilTheme.colors.accentPrimary,
                unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft,
                focusedTextColor = VakilTheme.colors.textPrimary,
                unfocusedTextColor = VakilTheme.colors.textPrimary,
                focusedLabelColor = VakilTheme.colors.accentPrimary,
                unfocusedLabelColor = VakilTheme.colors.textTertiary
            ),
            textStyle = VakilTheme.typography.bodyLarge
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(VakilTheme.colors.bgElevated)
        ) {
            cases.forEach { item ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "${item.caseName} • ${item.caseNumber}",
                            style = VakilTheme.typography.bodyMedium,
                            color = VakilTheme.colors.textPrimary
                        ) 
                    },
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
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = taskTypeLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Task Category", style = VakilTheme.typography.bodyMedium) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VakilTheme.colors.accentPrimary,
                unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft,
                focusedTextColor = VakilTheme.colors.textPrimary,
                unfocusedTextColor = VakilTheme.colors.textPrimary,
                focusedLabelColor = VakilTheme.colors.accentPrimary,
                unfocusedLabelColor = VakilTheme.colors.textTertiary
            ),
            textStyle = VakilTheme.typography.bodyLarge
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(VakilTheme.colors.bgElevated)
        ) {
            TaskType.values().forEach { type ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = taskTypeLabel(type),
                            style = VakilTheme.typography.bodyMedium,
                            color = VakilTheme.colors.textPrimary
                        ) 
                    },
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
        TaskType.COLLECT_PAPERS -> "Collect Papers"
        TaskType.VIEW_ORDERSHEET -> "View Ordersheet"
        TaskType.PHOTOCOPY -> "Get Photocopies"
        TaskType.PAY_COURT_FEES -> "Pay Court Fees"
        TaskType.PREPARE_ARGUMENTS -> "Prepare Arguments"
        TaskType.MEETING -> "Meeting"
        TaskType.CUSTOM -> "Custom Task"
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
    return date.format(DateTimeFormatter.ofPattern("dd MMMM, yyyy"))
}

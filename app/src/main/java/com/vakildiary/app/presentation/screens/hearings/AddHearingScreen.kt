package com.vakildiary.app.presentation.screens.hearings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.R
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.notifications.NotificationScheduler
import com.vakildiary.app.presentation.components.AnimatedSuccessDialog
import com.vakildiary.app.presentation.components.ButtonLabel
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.viewmodels.AddHearingViewModel
import com.vakildiary.app.presentation.viewmodels.state.AddHearingUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHearingScreen(
    preselectedCaseId: String? = null,
    onBack: () -> Unit = {},
    viewModel: AddHearingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCase by remember { mutableStateOf<Case?>(null) }
    var purpose by remember { mutableStateOf("") }
    var reminderMinutes by remember { mutableStateOf("60") }
    var dateMillis by remember { mutableStateOf<Long?>(null) }
    var timeText by remember { mutableStateOf("10:00") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadCases() }

    if (preselectedCaseId != null && selectedCase == null && uiState is AddHearingUiState.Success) {
        selectedCase = (uiState as AddHearingUiState.Success).cases.firstOrNull { it.caseId == preselectedCaseId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Schedule Hearing", style = VakilTheme.typography.headlineMedium) },
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
            when (uiState) {
                AddHearingUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VakilTheme.colors.accentPrimary)
                is AddHearingUiState.Error -> Text(
                    text = (uiState as AddHearingUiState.Error).message,
                    color = VakilTheme.colors.error,
                    style = VakilTheme.typography.bodyMedium
                )
                is AddHearingUiState.Success -> {
                    CaseDropdown(
                        cases = (uiState as AddHearingUiState.Success).cases,
                        selected = selectedCase,
                        onSelected = { selectedCase = it }
                    )
                }
            }

            FormSection("Hearing Details") {
                AppTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = "Purpose of Hearing (e.g. Final Arguments)"
                )
                
                AppTextField(
                    value = dateMillis?.let { formatDate(it) }.orEmpty(),
                    onValueChange = {},
                    label = "Date*",
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

            if (!validationError.isNullOrBlank()) {
                Surface(
                    color = VakilTheme.colors.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError.orEmpty(),
                        color = VakilTheme.colors.error,
                        style = VakilTheme.typography.labelSmall,
                        modifier = Modifier.padding(VakilTheme.spacing.sm)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val case = selectedCase ?: return@Button
                    val date = dateMillis ?: return@Button
                    val minutes = reminderMinutes.toIntOrNull() ?: 0
                    val hearingAt = combineDateAndTime(date, timeText)
                    if (hearingAt < System.currentTimeMillis()) {
                        validationError = "Hearing date must be in the future"
                        return@Button
                    }
                    viewModel.saveHearing(
                        caseId = case.caseId,
                        hearingDateMillis = hearingAt,
                        purpose = purpose,
                        reminderMinutesBefore = minutes
                    )
                    validationError = null
                    val trigger = hearingAt - minutes * 60_000L
                    NotificationScheduler.scheduleHearingReminder(context, case.caseId, trigger)
                    showSuccessDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCase != null && dateMillis != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VakilTheme.colors.accentPrimary,
                    disabledContainerColor = VakilTheme.colors.bgSurfaceSoft
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(VakilTheme.spacing.md)
            ) {
                ButtonLabel(
                    text = "Save Hearing Schedule",
                    style = VakilTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
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
                }) {
                    ButtonLabel(
                        text = "Confirm",
                        style = VakilTheme.typography.labelMedium.copy(color = VakilTheme.colors.accentPrimary)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    ButtonLabel(
                        text = "Cancel",
                        style = VakilTheme.typography.labelMedium.copy(color = VakilTheme.colors.textSecondary)
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = VakilTheme.colors.bgElevated
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showSuccessDialog) {
        AnimatedSuccessDialog(
            title = stringResource(id = R.string.action_success_title),
            message = stringResource(id = R.string.hearing_updated_success_message),
            confirmText = stringResource(id = R.string.case_register_ok),
            onConfirm = onBack
        )
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
    selected: Case?,
    onSelected: (Case) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected?.let { "${it.caseName} • ${it.caseNumber}" }.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Associate with Case", style = VakilTheme.typography.bodyMedium) },
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
            cases.forEach { case ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "${case.caseName} • ${case.caseNumber}",
                            style = VakilTheme.typography.bodyMedium,
                            color = VakilTheme.colors.textPrimary
                        ) 
                    },
                    onClick = {
                        onSelected(case)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd MMMM, yyyy"))
}

private fun combineDateAndTime(dateMillis: Long, timeText: String): Long {
    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val parts = timeText.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 10
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val dateTime = date.atTime(LocalTime.of(hour, minute))
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

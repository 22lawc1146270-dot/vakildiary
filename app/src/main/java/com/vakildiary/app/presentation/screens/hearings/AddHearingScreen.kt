package com.vakildiary.app.presentation.screens.hearings

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.notifications.NotificationScheduler
import com.vakildiary.app.presentation.viewmodels.AddHearingViewModel
import com.vakildiary.app.presentation.viewmodels.state.AddHearingUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
    var validationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadCases() }

    if (preselectedCaseId != null && selectedCase == null && uiState is AddHearingUiState.Success) {
        selectedCase = (uiState as AddHearingUiState.Success).cases.firstOrNull { it.caseId == preselectedCaseId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Hearing") },
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
            when (uiState) {
                AddHearingUiState.Loading -> Text(text = "Loading cases...")
                is AddHearingUiState.Error -> Text(
                    text = (uiState as AddHearingUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                is AddHearingUiState.Success -> {
                    CaseDropdown(
                        cases = (uiState as AddHearingUiState.Success).cases,
                        selected = selectedCase,
                        onSelected = { selectedCase = it }
                    )
                }
            }

            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text(text = "Purpose") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dateMillis?.let { "Selected" }.orEmpty(),
                onValueChange = {},
                label = { Text(text = "Hearing Date") },
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

            if (!validationError.isNullOrBlank()) {
                Text(text = validationError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

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
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(text = "Save Hearing")
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
    selected: Case?,
    onSelected: (Case) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected?.caseName.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Select Case") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            cases.forEach { case ->
                DropdownMenuItem(
                    text = { Text(text = case.caseName) },
                    onClick = {
                        onSelected(case)
                        expanded = false
                    }
                )
            }
        }
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

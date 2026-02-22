package com.vakildiary.app.presentation.screens.meetings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vakildiary.app.presentation.viewmodels.MeetingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeetingScreen(
    caseId: String,
    onBack: () -> Unit = {},
    viewModel: MeetingViewModel = hiltViewModel()
) {
    var clientName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var agenda by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var reminderMinutes by remember { mutableStateOf("60") }
    var meetingDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Meeting") },
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
            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text(text = "Client Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(text = "Location") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = agenda,
                onValueChange = { agenda = it },
                label = { Text(text = "Agenda") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(text = "Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = reminderMinutes,
                onValueChange = { reminderMinutes = it },
                label = { Text(text = "Reminder (minutes before)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = meetingDateMillis?.let { "Selected" }.orEmpty(),
                onValueChange = {},
                label = { Text(text = "Meeting Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(imageVector = Icons.Default.Event, contentDescription = "Pick date")
                    }
                }
            )

            Button(
                onClick = {
                    val reminder = reminderMinutes.toIntOrNull() ?: 0
                    val date = meetingDateMillis ?: System.currentTimeMillis()
                    viewModel.addMeeting(
                        caseId = caseId,
                        clientName = clientName,
                        meetingDate = date,
                        location = location,
                        agenda = agenda,
                        notes = notes,
                        reminderMinutesBefore = reminder
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(text = "Save Meeting")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    meetingDateMillis = datePickerState.selectedDateMillis
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

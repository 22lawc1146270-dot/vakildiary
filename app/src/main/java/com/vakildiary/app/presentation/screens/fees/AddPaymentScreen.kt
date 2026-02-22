package com.vakildiary.app.presentation.screens.fees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.PaymentMode
import com.vakildiary.app.presentation.viewmodels.AddPaymentViewModel
import com.vakildiary.app.presentation.viewmodels.state.AddPaymentUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentScreen(
    caseId: String,
    onBack: () -> Unit,
    viewModel: AddPaymentViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val outstandingAmount by viewModel.outstandingAmount.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onReceiptSelected(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Payment") },
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
            if (uiState is AddPaymentUiState.Error) {
                Text(
                    text = (uiState as AddPaymentUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (outstandingAmount != null) {
                Text(
                    text = "Outstanding: ₹${outstandingAmount!!.toInt()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = formState.amount,
                onValueChange = viewModel::onAmountChanged,
                label = { Text(text = "Amount*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = formState.paymentDateMillis?.let { formatDate(it) }.orEmpty(),
                onValueChange = {},
                label = { Text(text = "Payment Date*") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(imageVector = Icons.Default.Event, contentDescription = "Pick date")
                    }
                }
            )

            PaymentModeDropdown(
                selected = formState.paymentMode,
                onSelected = viewModel::onPaymentModeChanged
            )

            OutlinedTextField(
                value = formState.referenceNumber,
                onValueChange = viewModel::onReferenceNumberChanged,
                label = { Text(text = "Reference Number") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { receiptPicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach receipt")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (formState.receiptPath.isBlank()) "Attach Receipt Photo" else "Change Receipt Photo")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isValid() && uiState !is AddPaymentUiState.Loading,
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(text = if (uiState is AddPaymentUiState.Loading) "Saving..." else "Save")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        viewModel.onDateChanged(selected)
                    }
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

    LaunchedEffect(uiState) {
        if (uiState is AddPaymentUiState.Success && (uiState as AddPaymentUiState.Success).isSaved) {
            onBack()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentModeDropdown(
    selected: PaymentMode?,
    onSelected: (PaymentMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Payment Mode*") },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PaymentMode.values().forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(text = option.name) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

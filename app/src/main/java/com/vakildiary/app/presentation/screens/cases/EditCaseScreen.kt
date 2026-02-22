package com.vakildiary.app.presentation.screens.cases

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.presentation.viewmodels.EditCaseViewModel
import com.vakildiary.app.presentation.viewmodels.state.AddCaseUiState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCaseScreen(
    caseId: String,
    onBack: () -> Unit = {},
    viewModel: EditCaseViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(caseId) {
        viewModel.load(caseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Case") },
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
            if (uiState is AddCaseUiState.Error) {
                Text(
                    text = (uiState as AddCaseUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = formState.caseName,
                onValueChange = viewModel::onCaseNameChanged,
                label = { Text(text = "Case Name*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.caseNumber,
                onValueChange = viewModel::onCaseNumberChanged,
                label = { Text(text = "Case Number*") },
                modifier = Modifier.fillMaxWidth()
            )

            EnumDropdownField(
                label = "Court Type*",
                options = CourtType.values().toList(),
                selected = formState.courtType,
                onSelected = viewModel::onCourtTypeChanged
            ) { it.name }

            OutlinedTextField(
                value = formState.courtName,
                onValueChange = viewModel::onCourtNameChanged,
                label = { Text(text = "Court Name*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.clientName,
                onValueChange = viewModel::onClientNameChanged,
                label = { Text(text = "Client Name*") },
                modifier = Modifier.fillMaxWidth()
            )

            EnumDropdownField(
                label = "Case Type*",
                options = CaseType.values().toList(),
                selected = formState.caseType,
                onSelected = viewModel::onCaseTypeChanged
            ) { it.name }

            EnumDropdownField(
                label = "Case Stage*",
                options = CaseStage.values().toList(),
                selected = formState.caseStage,
                onSelected = viewModel::onCaseStageChanged
            ) { it.name }

            OutlinedTextField(
                value = formState.oppositeParty,
                onValueChange = viewModel::onOppositePartyChanged,
                label = { Text(text = "Opposite Party") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.assignedJudge,
                onValueChange = viewModel::onJudgeChanged,
                label = { Text(text = "Judge Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.firNumber,
                onValueChange = viewModel::onFirNumberChanged,
                label = { Text(text = "FIR Number") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.actsAndSections,
                onValueChange = viewModel::onActsChanged,
                label = { Text(text = "Acts & Sections") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.clientPhone,
                onValueChange = viewModel::onPhoneChanged,
                label = { Text(text = "Client Phone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            OutlinedTextField(
                value = formState.clientEmail,
                onValueChange = viewModel::onEmailChanged,
                label = { Text(text = "Client Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = formState.totalAgreedFees,
                onValueChange = viewModel::onFeesChanged,
                label = { Text(text = "Total Agreed Fees") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = formState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text(text = "Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AddCaseUiState.Loading,
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(text = if (uiState is AddCaseUiState.Loading) "Saving..." else "Save Changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    labelFor: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selected?.let(labelFor).orEmpty()

    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(text = labelFor(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

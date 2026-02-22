package com.vakildiary.app.presentation.screens.ecourt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.presentation.model.ECourtCaseItem
import com.vakildiary.app.presentation.model.ECourtSearchForm
import com.vakildiary.app.presentation.viewmodels.ECourtSearchUiState
import com.vakildiary.app.presentation.viewmodels.ECourtSearchViewModel
import com.vakildiary.app.presentation.util.rememberIsOnline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ECourtSearchScreen(
    onBack: () -> Unit,
    onImport: (ECourtCaseItem, ECourtSearchForm) -> Unit,
    viewModel: ECourtSearchViewModel = hiltViewModel()
) {
    var form by remember { mutableStateOf(ECourtSearchForm()) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by rememberIsOnline()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "eCourt Search") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EnumDropdownField(
                label = "Court Type",
                options = CourtType.values().toList(),
                selected = form.courtType,
                onSelected = { form = form.copy(courtType = it) }
            ) { it.name }

            OutlinedTextField(
                value = form.courtName,
                onValueChange = { form = form.copy(courtName = it) },
                label = { Text(text = "Court Name") },
                modifier = Modifier.fillMaxWidth()
            )

            if (!isOnline) {
                Text(
                    text = "No internet connection. eCourt features unavailable.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = form.stateCode,
                onValueChange = { form = form.copy(stateCode = it) },
                label = { Text(text = "State Code*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.districtCode,
                onValueChange = { form = form.copy(districtCode = it) },
                label = { Text(text = "District Code*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.courtCode,
                onValueChange = { form = form.copy(courtCode = it) },
                label = { Text(text = "Court Code*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.caseType,
                onValueChange = { form = form.copy(caseType = it) },
                label = { Text(text = "Case Type Code*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.caseNumber,
                onValueChange = { form = form.copy(caseNumber = it) },
                label = { Text(text = "Case Number*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = form.year,
                onValueChange = { form = form.copy(year = it) },
                label = { Text(text = "Year*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = form.captcha,
                onValueChange = { form = form.copy(captcha = it) },
                label = { Text(text = "Captcha*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.csrfMagic,
                onValueChange = { form = form.copy(csrfMagic = it) },
                label = { Text(text = "CSRF Magic (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.search(form) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isOnline
            ) {
                Text(text = "Search")
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                ECourtSearchUiState.Loading -> CircularProgressIndicator()
                is ECourtSearchUiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error)
                is ECourtSearchUiState.Success -> {
                    if (state.results.isNotEmpty()) {
                        Text(text = "Results", style = MaterialTheme.typography.titleMedium)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.results) { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = item.caseTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(text = item.parties, style = MaterialTheme.typography.bodyMedium)
                                    if (item.nextHearingDate.isNotBlank()) {
                                        Text(text = "Next hearing: ${item.nextHearingDate}")
                                    }
                                    if (item.stage.isNotBlank()) {
                                        Text(text = "Stage: ${item.stage}")
                                    }
                                    Button(
                                        onClick = { onImport(item, form) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "Import Case")
                                    }
                                }
                            }
                        }
                    }
                }
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
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

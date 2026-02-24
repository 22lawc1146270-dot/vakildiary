package com.vakildiary.app.presentation.screens.ecourt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.domain.model.ECourtComplexOption
import com.vakildiary.app.domain.model.ECourtOption
import com.vakildiary.app.R
import com.vakildiary.app.presentation.model.ECourtCaseItem
import com.vakildiary.app.presentation.model.ECourtSearchForm
import com.vakildiary.app.presentation.viewmodels.ECourtLookupUiState
import com.vakildiary.app.presentation.viewmodels.ECourtSearchUiState
import com.vakildiary.app.presentation.viewmodels.ECourtSearchViewModel
import com.vakildiary.app.presentation.util.rememberIsOnline
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ECourtSearchScreen(
    onBack: () -> Unit,
    onImport: (ECourtCaseItem, ECourtSearchForm) -> Unit,
    viewModel: ECourtSearchViewModel = hiltViewModel()
) {
    var form by remember { mutableStateOf(ECourtSearchForm()) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lookupState by viewModel.lookupState.collectAsStateWithLifecycle()
    val captchaUrl by viewModel.captchaUrl.collectAsStateWithLifecycle()
    val isOnline by rememberIsOnline()
    var showResultsSheet by remember { mutableStateOf(false) }
    val years = remember {
        val currentYear = Year.now().value
        (currentYear downTo currentYear - 30).map { it.toString() }
    }

    LaunchedEffect(years) {
        if (form.year.isBlank()) {
            form = form.copy(year = years.first())
        }
    }

    LaunchedEffect(uiState) {
        showResultsSheet = when (uiState) {
            is ECourtSearchUiState.Success -> (uiState as ECourtSearchUiState.Success).results.isNotEmpty()
            else -> false
        }
    }

    val lookupData = (lookupState as? ECourtLookupUiState.Success)?.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.ecourt_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back))
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
            if (!isOnline) {
                OfflineBanner(onRetry = { if (isOnline) viewModel.loadSession() })
            }

            when (lookupState) {
                ECourtLookupUiState.Loading -> CircularProgressIndicator()
                is ECourtLookupUiState.Error -> Text(
                    text = (lookupState as ECourtLookupUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                else -> Unit
            }

            EnumDropdownField(
                label = stringResource(id = R.string.ecourt_court_type),
                options = CourtType.values().toList(),
                selected = form.courtType,
                onSelected = { form = form.copy(courtType = it) }
            ) { it.name.replaceFirstChar(Char::uppercase) }

            OptionDropdownField(
                label = stringResource(id = R.string.ecourt_state_code),
                options = lookupData?.states.orEmpty(),
                selected = lookupData?.states?.firstOrNull { it.code == form.stateCode },
                enabled = lookupData?.states?.isNotEmpty() == true
            ) { option ->
                form = form.copy(
                    stateCode = option.code,
                    districtCode = "",
                    courtCode = "",
                    establishmentCode = "",
                    caseType = "",
                    courtName = ""
                )
                viewModel.loadDistricts(option.code)
            }

            OptionDropdownField(
                label = stringResource(id = R.string.ecourt_district_code),
                options = lookupData?.districts.orEmpty(),
                selected = lookupData?.districts?.firstOrNull { it.code == form.districtCode },
                enabled = form.stateCode.isNotBlank() && lookupData?.districts?.isNotEmpty() == true
            ) { option ->
                form = form.copy(
                    districtCode = option.code,
                    courtCode = "",
                    establishmentCode = "",
                    caseType = "",
                    courtName = ""
                )
                viewModel.loadCourtComplexes(form.stateCode, option.code)
            }

            ComplexDropdownField(
                label = stringResource(id = R.string.ecourt_court_name),
                options = lookupData?.courts.orEmpty(),
                selected = lookupData?.courts?.firstOrNull { it.complexCode == form.courtCode },
                enabled = form.districtCode.isNotBlank() && lookupData?.courts?.isNotEmpty() == true
            ) { option ->
                form = form.copy(
                    courtCode = option.complexCode,
                    courtName = option.label,
                    establishmentCode = "",
                    caseType = ""
                )
                if (option.requiresEstablishment) {
                    viewModel.loadEstablishments(form.stateCode, form.districtCode, option.complexCode)
                } else {
                    viewModel.loadCaseTypes(form.stateCode, form.districtCode, option.complexCode, null)
                }
            }

            if (lookupData?.requiresEstablishment == true) {
                OptionDropdownField(
                    label = stringResource(id = R.string.ecourt_court_code),
                    options = lookupData.establishments,
                    selected = lookupData.establishments.firstOrNull { it.code == form.establishmentCode },
                    enabled = lookupData.establishments.isNotEmpty()
                ) { option ->
                    form = form.copy(
                        establishmentCode = option.code,
                        caseType = ""
                    )
                    viewModel.loadCaseTypes(form.stateCode, form.districtCode, form.courtCode, option.code)
                }
            } else if (form.courtCode.isNotBlank()) {
                OutlinedTextField(
                    value = form.courtCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = stringResource(id = R.string.ecourt_court_code)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OptionDropdownField(
                label = stringResource(id = R.string.ecourt_case_type_code),
                options = lookupData?.caseTypes.orEmpty(),
                selected = lookupData?.caseTypes?.firstOrNull { it.code == form.caseType },
                enabled = lookupData?.caseTypes?.isNotEmpty() == true
            ) { option ->
                form = form.copy(caseType = option.code)
            }

            YearDropdownField(
                label = stringResource(id = R.string.ecourt_year),
                options = years,
                selected = form.year,
                enabled = true
            ) { year ->
                form = form.copy(year = year)
            }

            OutlinedTextField(
                value = form.caseNumber,
                onValueChange = { form = form.copy(caseNumber = it) },
                label = { Text(text = stringResource(id = R.string.ecourt_case_number)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )

            CaptchaField(
                captchaUrl = captchaUrl,
                captchaValue = form.captcha,
                onCaptchaChanged = { form = form.copy(captcha = it) },
                onRefresh = viewModel::refreshCaptcha
            )

            Button(
                onClick = { viewModel.search(form) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isOnline
            ) {
                Text(text = stringResource(id = R.string.ecourt_search_button))
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                ECourtSearchUiState.Loading -> CircularProgressIndicator()
                is ECourtSearchUiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error)
                is ECourtSearchUiState.Success -> Unit
            }
        }
    }

    val results = (uiState as? ECourtSearchUiState.Success)?.results.orEmpty()
    if (showResultsSheet && results.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showResultsSheet = false }) {
            Text(
                text = stringResource(id = R.string.ecourt_results),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results) { item ->
                    ResultCard(
                        item = item,
                        onImport = {
                            onImport(item, form)
                            showResultsSheet = false
                        }
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionDropdownField(
    label: String,
    options: List<ECourtOption>,
    selected: ECourtOption?,
    enabled: Boolean,
    onSelected: (ECourtOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selected?.let { "${it.label} (${it.code})" }.orEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            enabled = enabled,
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
                    text = { Text(text = "${option.label} (${option.code})") },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComplexDropdownField(
    label: String,
    options: List<ECourtComplexOption>,
    selected: ECourtComplexOption?,
    enabled: Boolean,
    onSelected: (ECourtComplexOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selected?.let { "${it.label} (${it.complexCode})" }.orEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            enabled = enabled,
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
                    text = { Text(text = "${option.label} (${option.complexCode})") },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearDropdownField(
    label: String,
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            enabled = enabled,
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
                    text = { Text(text = option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner(onRetry: () -> Unit) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(id = R.string.ecourt_offline_title))
                Text(
                    text = stringResource(id = R.string.ecourt_offline_subtitle),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = onRetry) {
                Text(text = stringResource(id = R.string.ecourt_retry))
            }
        }
    }
}

@Composable
private fun CaptchaField(
    captchaUrl: String,
    captchaValue: String,
    onCaptchaChanged: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = captchaUrl,
                contentDescription = stringResource(id = R.string.ecourt_captcha_image),
                modifier = Modifier.size(140.dp)
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.ecourt_captcha_refresh)
                )
            }
        }
        OutlinedTextField(
            value = captchaValue,
            onValueChange = onCaptchaChanged,
            label = { Text(text = stringResource(id = R.string.ecourt_captcha)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )
    }
}

@Composable
private fun ResultCard(
    item: ECourtCaseItem,
    onImport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = item.caseTitle, style = MaterialTheme.typography.titleMedium)
            if (item.parties.isNotBlank()) {
                Text(text = item.parties, style = MaterialTheme.typography.bodyMedium)
            }
            if (item.nextHearingDate.isNotBlank()) {
                Text(text = "${stringResource(id = R.string.ecourt_next_hearing)}: ${item.nextHearingDate}")
            }
            if (item.stage.isNotBlank()) {
                Text(text = "${stringResource(id = R.string.ecourt_stage)}: ${item.stage}")
            }
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.ecourt_import_case))
            }
        }
    }
}

package com.vakildiary.app.presentation.screens.cases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.viewmodels.AddCaseViewModel
import com.vakildiary.app.presentation.viewmodels.state.AddCaseUiState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCaseScreen(
    prefillCaseName: String? = null,
    prefillCaseNumber: String? = null,
    prefillCourtName: String? = null,
    prefillClientName: String? = null,
    prefillCourtType: CourtType? = null,
    prefillCaseType: CaseType? = null,
    prefillCaseStage: CaseStage? = null,
    prefillEcourtStateCode: String? = null,
    prefillEcourtDistrictCode: String? = null,
    prefillEcourtCourtCode: String? = null,
    prefillEcourtEstablishmentCode: String? = null,
    prefillEcourtCaseTypeCode: String? = null,
    prefillEcourtYear: String? = null,
    onBack: () -> Unit = {},
    viewModel: AddCaseViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(
        prefillCaseName, prefillCaseNumber, prefillCourtName, prefillClientName,
        prefillCourtType, prefillCaseType, prefillCaseStage,
        prefillEcourtStateCode, prefillEcourtDistrictCode, prefillEcourtCourtCode,
        prefillEcourtEstablishmentCode, prefillEcourtCaseTypeCode, prefillEcourtYear
    ) {
        if (!prefillCaseName.isNullOrBlank() || !prefillCaseNumber.isNullOrBlank() ||
            !prefillCourtName.isNullOrBlank() || !prefillClientName.isNullOrBlank() ||
            prefillCourtType != null || prefillCaseType != null || prefillCaseStage != null
        ) {
            viewModel.prefill(
                caseName = prefillCaseName,
                caseNumber = prefillCaseNumber,
                courtName = prefillCourtName,
                clientName = prefillClientName,
                courtType = prefillCourtType,
                caseType = prefillCaseType,
                caseStage = prefillCaseStage
            )
        }
        if (!prefillEcourtStateCode.isNullOrBlank() && !prefillEcourtDistrictCode.isNullOrBlank() &&
            !prefillEcourtCourtCode.isNullOrBlank() && !prefillEcourtCaseTypeCode.isNullOrBlank() &&
            !prefillEcourtYear.isNullOrBlank()
        ) {
            viewModel.setECourtTracking(
                stateCode = prefillEcourtStateCode,
                districtCode = prefillEcourtDistrictCode,
                courtCode = prefillEcourtCourtCode,
                establishmentCode = prefillEcourtEstablishmentCode,
                caseTypeCode = prefillEcourtCaseTypeCode,
                year = prefillEcourtYear,
                courtName = prefillCourtName.orEmpty(),
                courtType = prefillCourtType,
                caseNumber = prefillCaseNumber.orEmpty()
            )
        } else {
            viewModel.clearECourtTracking()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.warning.collectLatest { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "New Case", style = VakilTheme.typography.headlineMedium) },
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
            verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)
        ) {
            if (uiState is AddCaseUiState.Error) {
                Surface(
                    color = VakilTheme.colors.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as AddCaseUiState.Error).message,
                        color = VakilTheme.colors.error,
                        style = VakilTheme.typography.bodyMedium,
                        modifier = Modifier.padding(VakilTheme.spacing.sm)
                    )
                }
            }

            FormSection("Basic Information") {
                AppTextField(
                    value = formState.caseName,
                    onValueChange = viewModel::onCaseNameChanged,
                    label = "Case Title*"
                )
                AppTextField(
                    value = formState.caseNumber,
                    onValueChange = viewModel::onCaseNumberChanged,
                    label = "Case Number*"
                )
                EnumDropdownField(
                    label = "Court Type*",
                    options = CourtType.values().toList(),
                    selected = formState.courtType,
                    onSelected = viewModel::onCourtTypeChanged
                ) { it.name }
                AppTextField(
                    value = formState.courtName,
                    onValueChange = viewModel::onCourtNameChanged,
                    label = "Full Court Name*"
                )
                AppTextField(
                    value = formState.clientName,
                    onValueChange = viewModel::onClientNameChanged,
                    label = "Client Name*"
                )
            }

            FormSection("Case Details") {
                EnumDropdownField(
                    label = "Case Category*",
                    options = CaseType.values().toList(),
                    selected = formState.caseType,
                    onSelected = viewModel::onCaseTypeChanged
                ) { it.name }
                EnumDropdownField(
                    label = "Current Stage*",
                    options = CaseStage.values().toList(),
                    selected = formState.caseStage,
                    onSelected = viewModel::onCaseStageChanged
                ) { it.name }
                AppTextField(
                    value = formState.oppositeParty,
                    onValueChange = viewModel::onOppositePartyChanged,
                    label = "Opposite Party / Advocate"
                )
                AppTextField(
                    value = formState.assignedJudge,
                    onValueChange = viewModel::onJudgeChanged,
                    label = "Hon'ble Judge"
                )
            }

            FormSection("Legal Identifiers") {
                AppTextField(
                    value = formState.firNumber,
                    onValueChange = viewModel::onFirNumberChanged,
                    label = "FIR Number (if applicable)"
                )
                AppTextField(
                    value = formState.actsAndSections,
                    onValueChange = viewModel::onActsChanged,
                    label = "Relevant Acts & Sections"
                )
            }

            FormSection("Client Contact") {
                AppTextField(
                    value = formState.clientPhone,
                    onValueChange = viewModel::onPhoneChanged,
                    label = "Phone Number",
                    keyboardType = KeyboardType.Phone
                )
                AppTextField(
                    value = formState.clientEmail,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Email Address",
                    keyboardType = KeyboardType.Email
                )
            }

            FormSection("Financials") {
                AppTextField(
                    value = formState.totalAgreedFees,
                    onValueChange = viewModel::onFeesChanged,
                    label = "Total Agreed Fee (₹)",
                    keyboardType = KeyboardType.Number
                )
            }

            AppTextField(
                value = formState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = "Internal Notes",
                minLines = 3
            )

            Spacer(modifier = Modifier.height(VakilTheme.spacing.md))

            Button(
                onClick = viewModel::onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isValid() && uiState !is AddCaseUiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VakilTheme.colors.accentPrimary,
                    disabledContainerColor = VakilTheme.colors.bgSurfaceSoft
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(VakilTheme.spacing.md)
            ) {
                Text(
                    text = if (uiState is AddCaseUiState.Loading) "Processing..." else "Register Case",
                    style = VakilTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(VakilTheme.spacing.xl))
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)) {
        Text(
            text = title.uppercase(),
            style = VakilTheme.typography.labelSmall,
            color = VakilTheme.colors.accentPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
        )
        content()
        Spacer(modifier = Modifier.height(VakilTheme.spacing.xs))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, style = VakilTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VakilTheme.colors.accentPrimary,
            unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
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
            value = selectedLabel.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label, style = VakilTheme.typography.bodyMedium) },
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
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = labelFor(option).replace("_", " "),
                            style = VakilTheme.typography.bodyMedium,
                            color = VakilTheme.colors.textPrimary
                        ) 
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

package com.vakildiary.app.presentation.screens.judgments

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.R
import com.vakildiary.app.presentation.components.ButtonLabel
import com.vakildiary.app.presentation.model.ReportableCaseTypeOption
import com.vakildiary.app.presentation.model.ReportableFormInput
import com.vakildiary.app.presentation.viewmodels.ReportableDownloadUiState
import com.vakildiary.app.presentation.viewmodels.ReportableFormUiState
import com.vakildiary.app.presentation.viewmodels.ReportableJudgmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportableJudgmentScreen(
    judgmentId: String,
    caseNumber: String?,
    year: String?,
    onBack: () -> Unit,
    onOpenDocuments: () -> Unit,
    viewModel: ReportableJudgmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    var selectedCaseType by remember { mutableStateOf<ReportableCaseTypeOption?>(null) }
    var caseNumberValue by remember { mutableStateOf(caseNumber.orEmpty()) }
    var caseYearValue by remember { mutableStateOf(year.orEmpty()) }
    var diaryYearValue by remember { mutableStateOf(year.orEmpty()) }
    var captchaValue by remember { mutableStateOf("") }

    LaunchedEffect(caseNumber, year) {
        viewModel.loadForm(caseNumber, year)
    }

    LaunchedEffect(formState) {
        val data = (formState as? ReportableFormUiState.Success)?.data ?: return@LaunchedEffect
        if (selectedCaseType == null && data.suggestedCaseTypeValue != null) {
            selectedCaseType = data.caseTypes.firstOrNull { it.value == data.suggestedCaseTypeValue }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = context.getString(R.string.reportable_judgment_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val state = formState) {
                ReportableFormUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReportableFormUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadForm(caseNumber, year) }) {
                        ButtonLabel(text = stringResource(id = R.string.ecourt_retry))
                    }
                }
                is ReportableFormUiState.Success -> {
                    CaseTypeDropdown(
                        options = state.data.caseTypes,
                        selected = selectedCaseType,
                        onSelected = { selectedCaseType = it }
                    )

                    OutlinedTextField(
                        value = caseNumberValue,
                        onValueChange = { caseNumberValue = it },
                        label = { Text(text = stringResource(id = R.string.reportable_case_number)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )

                    if (state.data.requiresCaseYear) {
                        OutlinedTextField(
                            value = caseYearValue,
                            onValueChange = { caseYearValue = it },
                            label = { Text(text = stringResource(id = R.string.reportable_case_year)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    if (state.data.requiresDiaryYear) {
                        OutlinedTextField(
                            value = diaryYearValue,
                            onValueChange = { diaryYearValue = it },
                            label = { Text(text = stringResource(id = R.string.reportable_diary_year)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    CaptchaField(
                        captchaImage = state.data.captchaImage,
                        captchaValue = captchaValue,
                        onCaptchaChanged = { captchaValue = it },
                        onRefresh = { viewModel.loadForm(caseNumberValue, caseYearValue) }
                    )

                    Button(
                        onClick = {
                            val sessionId = state.data.sessionId
                            val formInput = ReportableFormInput(
                                sessionId = sessionId,
                                caseTypeValue = selectedCaseType?.value.orEmpty(),
                                caseNumber = caseNumberValue,
                                caseYear = caseYearValue.takeIf { state.data.requiresCaseYear },
                                diaryYear = diaryYearValue.takeIf { state.data.requiresDiaryYear },
                                captcha = captchaValue
                            )
                            val tagYear = caseYearValue.ifBlank { year.orEmpty() }
                            viewModel.submitForm(
                                input = formInput,
                                judgmentId = judgmentId,
                                year = tagYear,
                                caseNumber = caseNumberValue,
                                requiredMessage = context.getString(R.string.reportable_required_error)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ButtonLabel(text = stringResource(id = R.string.reportable_download_button))
                    }
                }
            }
        }
    }

    when (val state = downloadState) {
        is ReportableDownloadUiState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = context.getString(R.string.reportable_judgment_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = context.getString(R.string.reportable_download_in_progress))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {}
            )
        }
        is ReportableDownloadUiState.Success -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = context.getString(R.string.reportable_judgment_title)) },
                text = { Text(text = context.getString(R.string.reportable_download_success)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetState()
                        onOpenDocuments()
                    }) { Text(text = context.getString(android.R.string.ok)) }
                }
            )
        }
        is ReportableDownloadUiState.Error -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = context.getString(R.string.reportable_download_failed_title)) },
                text = { Text(text = state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetState() }) {
                        Text(text = context.getString(android.R.string.ok))
                    }
                }
            )
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseTypeDropdown(
    options: List<ReportableCaseTypeOption>,
    selected: ReportableCaseTypeOption?,
    onSelected: (ReportableCaseTypeOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selected?.label.orEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(id = R.string.reportable_case_type)) },
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
                    text = { Text(text = option.label) },
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
private fun CaptchaField(
    captchaImage: ByteArray,
    captchaValue: String,
    onCaptchaChanged: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(id = R.string.reportable_captcha_prompt))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val imageBitmap = remember(captchaImage) {
                BitmapFactory.decodeByteArray(captchaImage, 0, captchaImage.size)
                    ?.asImageBitmap()
            }
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = stringResource(id = R.string.reportable_captcha_label),
                    modifier = Modifier.size(140.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.reportable_captcha_refresh)
                )
            }
        }
        OutlinedTextField(
            value = captchaValue,
            onValueChange = onCaptchaChanged,
            label = { Text(text = stringResource(id = R.string.reportable_captcha_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

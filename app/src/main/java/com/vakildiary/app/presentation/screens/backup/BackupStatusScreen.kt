package com.vakildiary.app.presentation.screens.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.vakildiary.app.R
import com.vakildiary.app.domain.model.BackupLogEntry
import com.vakildiary.app.domain.model.BackupSchedule
import com.vakildiary.app.domain.model.BackupStatus
import com.vakildiary.app.presentation.viewmodels.BackupStatusViewModel
import com.vakildiary.app.presentation.viewmodels.RestoreViewModel
import com.vakildiary.app.presentation.viewmodels.state.RestoreUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupStatusScreen(
    onBack: () -> Unit,
    backupStatusViewModel: BackupStatusViewModel = hiltViewModel(),
    restoreViewModel: RestoreViewModel = hiltViewModel()
) {
    val schedule by backupStatusViewModel.backupSchedule.collectAsStateWithLifecycle()
    val lastBackupTime by backupStatusViewModel.lastBackupTime.collectAsStateWithLifecycle()
    val lastBackupSize by backupStatusViewModel.lastBackupSizeBytes.collectAsStateWithLifecycle()
    val lastBackupStatus by backupStatusViewModel.lastBackupStatus.collectAsStateWithLifecycle()
    val lastBackupMessage by backupStatusViewModel.lastBackupMessage.collectAsStateWithLifecycle()
    val backupLog by backupStatusViewModel.backupLog.collectAsStateWithLifecycle()
    val restoreState by restoreViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        restoreViewModel.checkForManualRestore()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.backup_status)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(stringResource(id = R.string.backup_schedule_label))
            ScheduleOption(
                label = stringResource(id = R.string.backup_manual_only),
                selected = schedule == BackupSchedule.MANUAL,
                onSelect = { backupStatusViewModel.setBackupSchedule(BackupSchedule.MANUAL) }
            )
            ScheduleOption(
                label = stringResource(id = R.string.backup_daily),
                selected = schedule == BackupSchedule.DAILY,
                onSelect = { backupStatusViewModel.setBackupSchedule(BackupSchedule.DAILY) }
            )
            ScheduleOption(
                label = stringResource(id = R.string.backup_weekly),
                selected = schedule == BackupSchedule.WEEKLY,
                onSelect = { backupStatusViewModel.setBackupSchedule(BackupSchedule.WEEKLY) }
            )

            SectionTitle(stringResource(id = R.string.backup_last_backup))
            Text(text = "${stringResource(id = R.string.backup_status_field)}: ${statusLabel(lastBackupStatus)}")
            Text(text = "${stringResource(id = R.string.backup_time_field)}: ${formatDateTime(lastBackupTime)}")
            Text(text = "${stringResource(id = R.string.backup_size_field)}: ${formatSize(lastBackupSize)}")
            if (!lastBackupMessage.isNullOrBlank()) {
                Text(text = "${stringResource(id = R.string.backup_message_field)}: $lastBackupMessage")
            }

            SectionTitle(stringResource(id = R.string.backup_log_label))
            if (backupLog.isEmpty()) {
                Text(text = stringResource(id = R.string.backup_log_empty))
            } else {
                backupLog.forEach { entry ->
                    LogRow(entry)
                }
            }

            SectionTitle(stringResource(id = R.string.backup_restore_label))
            if (restoreState is RestoreUiState.Error) {
                Text(
                    text = (restoreState as RestoreUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (restoreState is RestoreUiState.Restored) {
                Text(text = stringResource(id = R.string.backup_restore_complete), color = MaterialTheme.colorScheme.primary)
            }
            if (restoreState is RestoreUiState.NotAvailable) {
                Text(text = stringResource(id = R.string.backup_restore_none))
            }

            Button(
                onClick = { restoreViewModel.restoreNow() },
                modifier = Modifier.fillMaxWidth(),
                enabled = restoreState !is RestoreUiState.Restoring
            ) {
                Text(
                    text = if (restoreState is RestoreUiState.Restoring) {
                        stringResource(id = R.string.backup_restore_in_progress)
                    } else {
                        stringResource(id = R.string.backup_restore_button)
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ScheduleOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        RadioButton(selected = selected, onClick = onSelect)
    }
}

@Composable
private fun LogRow(entry: BackupLogEntry) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = formatDateTime(entry.timestamp), style = MaterialTheme.typography.bodyMedium)
        Text(text = "${statusLabel(entry.status)} • ${entry.message}", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun formatDateTime(timestamp: Long?): String {
    if (timestamp == null) return stringResource(id = R.string.backup_never)
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a"))
}

private fun formatSize(sizeBytes: Long?): String {
    if (sizeBytes == null) return "--"
    return when {
        sizeBytes >= 1_000_000 -> "${sizeBytes / 1_000_000} MB"
        sizeBytes >= 1_000 -> "${sizeBytes / 1_000} KB"
        else -> "$sizeBytes B"
    }
}

@Composable
private fun statusLabel(status: BackupStatus): String {
    return when (status) {
        BackupStatus.SUCCESS -> stringResource(id = R.string.backup_status_success)
        BackupStatus.ERROR -> stringResource(id = R.string.backup_status_failed)
        BackupStatus.NONE -> stringResource(id = R.string.backup_status_none)
    }
}

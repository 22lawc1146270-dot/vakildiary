package com.vakildiary.app.presentation.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.presentation.viewmodels.AuthViewModel
import com.vakildiary.app.presentation.viewmodels.BackupViewModel
import com.vakildiary.app.presentation.viewmodels.SettingsViewModel
import com.vakildiary.app.presentation.viewmodels.AppLockViewModel
import com.vakildiary.app.presentation.viewmodels.DeltaSyncViewModel
import com.vakildiary.app.presentation.viewmodels.state.BackupUiState
import com.vakildiary.app.presentation.theme.ThemeMode
import com.vakildiary.app.presentation.theme.LanguageMode
import androidx.compose.ui.res.stringResource
import com.vakildiary.app.R
import androidx.core.content.ContextCompat

@Composable
fun MoreScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    appLockViewModel: AppLockViewModel = hiltViewModel(),
    deltaSyncViewModel: DeltaSyncViewModel = hiltViewModel(),
    onOpenECourt: () -> Unit = {},
    onOpenJudgments: () -> Unit = {},
    onOpenBackupStatus: () -> Unit = {},
    onOpenUpcomingMeetings: () -> Unit = {}
) {
    val userEmail by authViewModel.userEmail.collectAsStateWithLifecycle()
    val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val languageMode by settingsViewModel.languageMode.collectAsStateWithLifecycle()
    val advocateName by settingsViewModel.advocateName.collectAsStateWithLifecycle()
    val isAppLockEnabled by appLockViewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val deltaSyncCount by deltaSyncViewModel.syncCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(id = R.string.more), style = MaterialTheme.typography.titleLarge)
        Text(text = "Signed in as: ${userEmail ?: "Guest"}")
        Button(
            onClick = { authViewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.sign_out))
        }
        if (userEmail.isNullOrBlank()) {
            PermissionCard(
                title = "Google Drive backup needs sign-in",
                actionLabel = "Grant permission",
                onAction = { authViewModel.resumeSignIn() }
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
            PermissionCard(
                title = "Notifications are off",
                actionLabel = "Grant permission",
                onAction = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
        }

        Text(text = stringResource(id = R.string.settings), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = advocateName ?: "",
            onValueChange = { settingsViewModel.setAdvocateName(it) },
            label = { Text(text = "Advocate Name (for sharing)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = "Integrations", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = onOpenECourt,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "eCourt Search")
        }
        Button(
            onClick = onOpenJudgments,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Judgment Search")
        }

        Button(
            onClick = onOpenBackupStatus,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.backup_status))
        }

        Button(
            onClick = onOpenUpcomingMeetings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Upcoming Meetings")
        }

        Button(
            onClick = { backupViewModel.backupNow() },
            modifier = Modifier.fillMaxWidth(),
            enabled = backupState !is BackupUiState.Loading
        ) {
            Text(text = if (backupState is BackupUiState.Loading) "Backing up..." else stringResource(id = R.string.backup_now))
        }

        Button(
            onClick = { deltaSyncViewModel.syncDocuments() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Delta Sync Now")
        }

        LaunchedEffect(deltaSyncCount) {
            if (deltaSyncCount > 0) {
                android.widget.Toast.makeText(
                    context,
                    "Delta sync complete: $deltaSyncCount files",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        Text(text = stringResource(id = R.string.theme), style = MaterialTheme.typography.titleMedium)
        ThemeOptionRow(
            label = stringResource(id = R.string.system_default),
            selected = themeMode == ThemeMode.SYSTEM,
            onSelect = { settingsViewModel.setThemeMode(ThemeMode.SYSTEM) }
        )
        ThemeOptionRow(
            label = stringResource(id = R.string.light),
            selected = themeMode == ThemeMode.LIGHT,
            onSelect = { settingsViewModel.setThemeMode(ThemeMode.LIGHT) }
        )
        ThemeOptionRow(
            label = stringResource(id = R.string.dark),
            selected = themeMode == ThemeMode.DARK,
            onSelect = { settingsViewModel.setThemeMode(ThemeMode.DARK) }
        )

        Text(text = stringResource(id = R.string.language), style = MaterialTheme.typography.titleMedium)
        ThemeOptionRow(
            label = stringResource(id = R.string.system_default),
            selected = languageMode == LanguageMode.SYSTEM,
            onSelect = { settingsViewModel.setLanguageMode(LanguageMode.SYSTEM) }
        )
        ThemeOptionRow(
            label = stringResource(id = R.string.english),
            selected = languageMode == LanguageMode.ENGLISH,
            onSelect = { settingsViewModel.setLanguageMode(LanguageMode.ENGLISH) }
        )
        ThemeOptionRow(
            label = stringResource(id = R.string.hindi),
            selected = languageMode == LanguageMode.HINDI,
            onSelect = { settingsViewModel.setLanguageMode(LanguageMode.HINDI) }
        )

        Text(text = stringResource(id = R.string.security), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(id = R.string.app_lock_biometric))
            Switch(
                checked = isAppLockEnabled,
                onCheckedChange = { appLockViewModel.setAppLockEnabled(it) }
            )
        }

        when (backupState) {
            is BackupUiState.Success -> Text(text = "Backup complete")
            is BackupUiState.Error -> Text(
                text = (backupState as BackupUiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            else -> Unit
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
            Text(text = actionLabel)
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        RadioButton(selected = selected, onClick = onSelect)
    }
}

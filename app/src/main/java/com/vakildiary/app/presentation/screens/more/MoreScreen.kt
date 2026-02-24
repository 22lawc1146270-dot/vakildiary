package com.vakildiary.app.presentation.screens.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.components.AppCard
import androidx.compose.ui.res.stringResource
import com.vakildiary.app.R
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.more), style = VakilTheme.typography.headlineMedium) },
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
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(VakilTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.lg)
        ) {
            // User Account Section
            AppCard {
                Column(modifier = Modifier.padding(VakilTheme.spacing.md)) {
                    Text(
                        text = "ACCOUNT",
                        style = VakilTheme.typography.labelSmall,
                        color = VakilTheme.colors.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(VakilTheme.spacing.sm))
                    Text(
                        text = userEmail ?: "Guest User",
                        style = VakilTheme.typography.bodyLarge,
                        color = VakilTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(VakilTheme.spacing.md))
                    Button(
                        onClick = { authViewModel.signOut() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VakilTheme.colors.bgSurfaceSoft),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.sign_out),
                            style = VakilTheme.typography.labelMedium,
                            color = VakilTheme.colors.error
                        )
                    }
                }
            }

            if (userEmail.isNullOrBlank()) {
                PermissionCard(
                    title = "Cloud backup disabled",
                    description = "Sign in to enable Google Drive sync and protect your data.",
                    actionLabel = "Sign In Now",
                    onAction = { authViewModel.resumeSignIn() }
                )
            }

            // Integrations & Tools
            SectionHeader("INTEGRATIONS")
            Column(verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)) {
                MoreMenuItem("eCourt Search", "Find cases on eCourts platform", onOpenECourt)
                MoreMenuItem("Judgment Search", "Access Supreme Court judgments", onOpenJudgments)
                MoreMenuItem("Cloud Sync", "Manage your backups and storage", onOpenBackupStatus)
                MoreMenuItem("Upcoming Meetings", "View your scheduled appointments", onOpenUpcomingMeetings)
            }

            // App Settings
            SectionHeader("PREFERENCES")
            AppCard {
                Column(modifier = Modifier.padding(VakilTheme.spacing.md)) {
                    OutlinedTextField(
                        value = advocateName ?: "",
                        onValueChange = { settingsViewModel.setAdvocateName(it) },
                        label = { Text("Advocate Name (for reports)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VakilTheme.colors.accentPrimary,
                            unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(VakilTheme.spacing.lg))
                    
                    Text(text = "Appearance", style = VakilTheme.typography.labelMedium, color = VakilTheme.colors.textSecondary)
                    ThemeOptionGroup(
                        options = listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.LIGHT_IVORY to "Ivory",
                            ThemeMode.LIGHT_NORDIC to "Nordic",
                            ThemeMode.DARK_SLATE to "Slate",
                            ThemeMode.DARK_ONYX to "Onyx"
                        ),
                        selected = themeMode,
                        onSelect = { settingsViewModel.setThemeMode(it) }
                    )

                    Spacer(modifier = Modifier.height(VakilTheme.spacing.lg))

                    Text(text = "Language", style = VakilTheme.typography.labelMedium, color = VakilTheme.colors.textSecondary)
                    LanguageOptionGroup(
                        options = listOf(
                            LanguageMode.SYSTEM to stringResource(id = R.string.system_default),
                            LanguageMode.ENGLISH to stringResource(id = R.string.english),
                            LanguageMode.HINDI to stringResource(id = R.string.hindi)
                        ),
                        selected = languageMode,
                        onSelect = { settingsViewModel.setLanguageMode(it) }
                    )
                }
            }

            // Security
            SectionHeader("SECURITY")
            AppCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(VakilTheme.spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_lock_biometric),
                            style = VakilTheme.typography.bodyLarge,
                            color = VakilTheme.colors.textPrimary
                        )
                        Text(
                            text = "Protect app with fingerprint",
                            style = VakilTheme.typography.labelSmall,
                            color = VakilTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { appLockViewModel.setAppLockEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VakilTheme.colors.onAccent,
                            checkedTrackColor = VakilTheme.colors.accentPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(VakilTheme.spacing.xl))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = VakilTheme.typography.labelSmall,
        color = VakilTheme.colors.textTertiary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = VakilTheme.spacing.xs)
    )
}

@Composable
private fun MoreMenuItem(title: String, subtitle: String, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VakilTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = VakilTheme.typography.bodyLarge, color = VakilTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = VakilTheme.typography.labelSmall, color = VakilTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    AppCard {
        Column(modifier = Modifier.padding(VakilTheme.spacing.md)) {
            Text(text = title, style = VakilTheme.typography.bodyLarge, color = VakilTheme.colors.warning, fontWeight = FontWeight.Bold)
            Text(text = description, style = VakilTheme.typography.bodyMedium, color = VakilTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(VakilTheme.spacing.md))
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VakilTheme.colors.accentPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = actionLabel, style = VakilTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ThemeOptionGroup(
    options: List<Pair<ThemeMode, String>>,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)
    ) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(mode) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VakilTheme.colors.accentPrimary,
                    selectedLabelColor = VakilTheme.colors.onAccent,
                    containerColor = VakilTheme.colors.bgElevated,
                    labelColor = VakilTheme.colors.textSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Color.Transparent else VakilTheme.colors.bgSurfaceSoft
                )
            )
        }
    }
}

@Composable
private fun LanguageOptionGroup(
    options: List<Pair<LanguageMode, String>>,
    selected: LanguageMode,
    onSelect: (LanguageMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)
    ) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(mode) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VakilTheme.colors.accentPrimary,
                    selectedLabelColor = VakilTheme.colors.onAccent,
                    containerColor = VakilTheme.colors.bgElevated,
                    labelColor = VakilTheme.colors.textSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Color.Transparent else VakilTheme.colors.bgSurfaceSoft
                )
            )
        }
    }
}

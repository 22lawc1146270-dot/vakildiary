package com.vakildiary.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.vakildiary.app.presentation.navigation.AppNavGraph
import com.vakildiary.app.presentation.navigation.Screen
import com.vakildiary.app.presentation.screens.auth.SignInScreen
import com.vakildiary.app.presentation.screens.docket.TodayDocketBottomSheet
import com.vakildiary.app.presentation.screens.docket.HearingOutcomeDialog
import com.vakildiary.app.presentation.theme.VakilDiaryTheme
import com.vakildiary.app.presentation.theme.ThemeMode
import com.vakildiary.app.presentation.theme.LanguageMode
import com.vakildiary.app.presentation.viewmodels.AuthViewModel
import com.vakildiary.app.presentation.viewmodels.DocketUiState
import com.vakildiary.app.presentation.viewmodels.TodayDocketViewModel
import com.vakildiary.app.presentation.viewmodels.RestoreViewModel
import com.vakildiary.app.presentation.viewmodels.SettingsViewModel
import com.vakildiary.app.presentation.viewmodels.AppLockViewModel
import com.vakildiary.app.presentation.viewmodels.OverdueTasksViewModel
import com.vakildiary.app.presentation.viewmodels.BackupStatusViewModel
import com.vakildiary.app.presentation.viewmodels.state.RestoreUiState
import com.vakildiary.app.security.BiometricLockManager
import com.vakildiary.app.notifications.ECourtSyncScheduler
import com.vakildiary.app.notifications.NotificationScheduler
import com.vakildiary.app.notifications.DeltaSyncScheduler
import com.vakildiary.app.notifications.BackupScheduler
import com.vakildiary.app.notifications.NotificationIntents
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationRoute = mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationRoute.value = parseNotificationRoute(intent)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val languageMode by settingsViewModel.languageMode.collectAsStateWithLifecycle()
            val notificationPromptShown by settingsViewModel.isNotificationPromptShown.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(languageMode) {
                val locales = when (languageMode) {
                    LanguageMode.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                    LanguageMode.ENGLISH -> LocaleListCompat.forLanguageTags("en")
                    LanguageMode.HINDI -> LocaleListCompat.forLanguageTags("hi")
                }
                AppCompatDelegate.setApplicationLocales(locales)
            }

            VakilDiaryTheme(darkTheme = isDarkTheme) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val userEmail by authViewModel.userEmail.collectAsStateWithLifecycle()
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                val isSignInSkipped by authViewModel.isSignInSkipped.collectAsStateWithLifecycle()
                val appLockViewModel: AppLockViewModel = hiltViewModel()
                val isAppLockEnabled by appLockViewModel.isAppLockEnabled.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val activity = context as? FragmentActivity
                var isUnlocked by rememberSaveable { mutableStateOf(false) }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { settingsViewModel.setNotificationPromptShown(true) }

                if (userEmail.isNullOrBlank() && !isSignInSkipped) {
                    SignInScreen(viewModel = authViewModel, uiState = authUiState)
                } else {
                    LaunchedEffect(Unit) {
                        NotificationScheduler.scheduleDailyDigest(this@MainActivity)
                        ECourtSyncScheduler.scheduleECourtSync(this@MainActivity)
                        DeltaSyncScheduler.scheduleDeltaSync(this@MainActivity)
                    }
                    val backupStatusViewModel: BackupStatusViewModel = hiltViewModel()
                    val backupSchedule by backupStatusViewModel.backupSchedule.collectAsStateWithLifecycle()
                    LaunchedEffect(backupSchedule) {
                        BackupScheduler.scheduleBackup(this@MainActivity, backupSchedule)
                    }

                    LaunchedEffect(notificationPromptShown) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !notificationPromptShown
                        ) {
                            val granted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                settingsViewModel.setNotificationPromptShown(true)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    if (isAppLockEnabled && !isUnlocked && activity != null) {
                        LaunchedEffect(isAppLockEnabled) {
                            BiometricLockManager().authenticate(
                                activity = activity,
                                title = "Unlock VakilDiary",
                                subtitle = "Authenticate to continue",
                                onSuccess = { isUnlocked = true },
                                onError = { }
                            )
                        }
                        return@VakilDiaryTheme
                    }

                    val restoreViewModel: RestoreViewModel = hiltViewModel()
                    val restoreUiState by restoreViewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(userEmail, isSignInSkipped) {
                        if (!userEmail.isNullOrBlank()) {
                            restoreViewModel.checkForRestore()
                        }
                    }

                    val navController = rememberNavController()
                    val deepLinkRoute = notificationRoute.value
                    LaunchedEffect(deepLinkRoute) {
                        val route = deepLinkRoute
                        if (!route.isNullOrBlank()) {
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                            notificationRoute.value = null
                        }
                    }
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val docketViewModel: TodayDocketViewModel = hiltViewModel()
                    val docketUiState by docketViewModel.uiState.collectAsStateWithLifecycle()
                    var isDocketSheetOpen by remember { mutableStateOf(false) }
                    var pendingOutcomeHearingId by remember { mutableStateOf<String?>(null) }
                    var pendingVoiceNotePath by remember { mutableStateOf<String?>(null) }
                    val voiceNoteRecorder = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        val uri = result.data?.data
                        pendingVoiceNotePath = uri?.let { copyVoiceNoteToInternal(it) }
                    }

                    val overdueTasksViewModel: OverdueTasksViewModel = hiltViewModel()
                    val overdueCount by overdueTasksViewModel.overdueCount.collectAsStateWithLifecycle()

                    val items = listOf(
                        BottomNavItem("Home", Icons.Default.Home, Screen.Dashboard, badgeCount = overdueCount),
                        BottomNavItem("Cases", Icons.Default.Work, Screen.CaseList),
                        BottomNavItem("Calendar", Icons.Default.Event, Screen.Calendar),
                        BottomNavItem("Documents", Icons.Default.Folder, Screen.Documents),
                        BottomNavItem("More", Icons.Default.Menu, Screen.More)
                    )

                    val pendingCount = when (docketUiState) {
                        is DocketUiState.Success -> {
                            val state = docketUiState as DocketUiState.Success
                            (state.totalCount - state.completedCount).coerceAtLeast(0)
                        }
                        else -> 0
                    }
                    val totalCount = (docketUiState as? DocketUiState.Success)?.totalCount ?: 0
                    val allDone = totalCount > 0 && pendingCount == 0

                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { isDocketSheetOpen = true },
                                containerColor = if (allDone) Color(0xFF1E7A4A) else Color(0xFFE67E22),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (pendingCount > 0) {
                                            Badge(containerColor = Color(0xFFC0392B)) {
                                                Text(text = pendingCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    val icon = if (allDone) Icons.Default.Check else Icons.Default.Gavel
                                    Icon(imageVector = icon, contentDescription = "Today's Docket")
                                }
                            }
                        },
                        bottomBar = {
                            NavigationBar {
                                items.forEach { item ->
                                    val selected = currentDestination
                                        ?.hierarchy
                                        ?.any { it.route == item.screen.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(item.screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (item.badgeCount > 0) {
                                                        Badge(containerColor = Color(0xFFC0392B)) {
                                                            Text(text = item.badgeCount.toString())
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(imageVector = item.icon, contentDescription = item.label)
                                            }
                                        },
                                        label = { Text(text = item.label) }
                                    )
                                }
                            }
                        }
                    ) { paddingValues ->
                        AppNavGraph(
                            navController = navController,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }

                    if (isDocketSheetOpen) {
                        TodayDocketBottomSheet(
                            uiState = docketUiState,
                            onDismiss = { isDocketSheetOpen = false },
                            onToggleHearing = { _, _ -> },
                            onHearingOutcome = { hearingId ->
                                pendingOutcomeHearingId = hearingId
                                pendingVoiceNotePath = null
                            },
                            onToggleTask = { taskId, isCompleted ->
                                if (isCompleted) {
                                    docketViewModel.markTaskComplete(taskId)
                                }
                            }
                        )
                    }

                    if (pendingOutcomeHearingId != null) {
                        val caseName = docketViewModel.getCaseName(pendingOutcomeHearingId!!).orEmpty()
                        HearingOutcomeDialog(
                            caseName = if (caseName.isBlank()) "Case" else caseName,
                            voiceNotePath = pendingVoiceNotePath,
                            onDismiss = { pendingOutcomeHearingId = null },
                            onAddVoiceNote = {
                                val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                                voiceNoteRecorder.launch(intent)
                            },
                            onSkipAndMarkDone = { outcome, orderDetails, adjournmentReason, nextDate ->
                                docketViewModel.markHearingComplete(
                                    hearingId = pendingOutcomeHearingId!!,
                                    outcome = outcome,
                                    orderDetails = orderDetails.ifBlank { null },
                                    adjournmentReason = adjournmentReason.ifBlank { null },
                                    voiceNotePath = pendingVoiceNotePath,
                                    nextDate = parseDate(nextDate)
                                )
                                pendingOutcomeHearingId = null
                                pendingVoiceNotePath = null
                            },
                            onSaveAndMarkDone = { outcome, orderDetails, adjournmentReason, nextDate ->
                                docketViewModel.markHearingComplete(
                                    hearingId = pendingOutcomeHearingId!!,
                                    outcome = outcome,
                                    orderDetails = orderDetails.ifBlank { null },
                                    adjournmentReason = adjournmentReason.ifBlank { null },
                                    voiceNotePath = pendingVoiceNotePath,
                                    nextDate = parseDate(nextDate)
                                )
                                pendingOutcomeHearingId = null
                                pendingVoiceNotePath = null
                            }
                        )
                    }

                    if (restoreUiState is RestoreUiState.Available || restoreUiState is RestoreUiState.Restoring || restoreUiState is RestoreUiState.Error) {
                        AlertDialog(
                            onDismissRequest = { restoreViewModel.skipRestore() },
                            title = { Text(text = "Restore from backup?") },
                            text = {
                                val message = when (restoreUiState) {
                                    is RestoreUiState.Restoring -> "Restoring your data..."
                                    is RestoreUiState.Error -> (restoreUiState as RestoreUiState.Error).message
                                    else -> "We found a backup in Google Drive. Would you like to restore now?"
                                }
                                Text(text = message)
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = { restoreViewModel.restoreNow() },
                                    enabled = restoreUiState !is RestoreUiState.Restoring
                                ) {
                                    Text(text = if (restoreUiState is RestoreUiState.Restoring) "Restoring..." else "Restore")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { restoreViewModel.skipRestore() }) {
                                    Text(text = "Not now")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        notificationRoute.value = parseNotificationRoute(intent)
    }

    private fun parseNotificationRoute(intent: Intent?): String? {
        val caseId = intent?.getStringExtra(NotificationIntents.EXTRA_CASE_ID)
        if (!caseId.isNullOrBlank()) {
            return Screen.CaseDetail.createRoute(caseId)
        }
        return when (intent?.getStringExtra(NotificationIntents.EXTRA_DESTINATION)) {
            NotificationIntents.DEST_OVERDUE -> Screen.OverdueTasks.route
            NotificationIntents.DEST_DASHBOARD -> Screen.Dashboard.route
            else -> null
        }
    }

    private fun copyVoiceNoteToInternal(uri: android.net.Uri): String? {
        return try {
            val dir = File(filesDir, "voice_notes")
            if (!dir.exists()) dir.mkdirs()
            val target = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            target.absolutePath
        } catch (t: Throwable) {
            null
        }
    }
}

private fun parseDate(dateText: String): Long? {
    return try {
        val parts = dateText.split("/")
        if (parts.size != 3) return null
        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val year = parts[2].toInt()
        java.time.LocalDate.of(year, month, day)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    } catch (t: Throwable) {
        null
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: Screen,
    val badgeCount: Int = 0
)

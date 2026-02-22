package com.vakildiary.app

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.AlertDialog
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
import com.vakildiary.app.presentation.viewmodels.state.RestoreUiState
import com.vakildiary.app.security.BiometricLockManager
import com.vakildiary.app.notifications.ECourtSyncScheduler
import com.vakildiary.app.notifications.NotificationScheduler
import com.vakildiary.app.notifications.DeltaSyncScheduler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val languageMode by settingsViewModel.languageMode.collectAsStateWithLifecycle()
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

                if (userEmail.isNullOrBlank() && !isSignInSkipped) {
                    SignInScreen(viewModel = authViewModel, uiState = authUiState)
                } else {
                    LaunchedEffect(Unit) {
                        NotificationScheduler.scheduleDailyDigest(this@MainActivity)
                        ECourtSyncScheduler.scheduleECourtSync(this@MainActivity)
                        DeltaSyncScheduler.scheduleDeltaSync(this@MainActivity)
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
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val docketViewModel: TodayDocketViewModel = hiltViewModel()
                    val docketUiState by docketViewModel.uiState.collectAsStateWithLifecycle()
                    var isDocketSheetOpen by remember { mutableStateOf(false) }
                    var pendingOutcomeHearingId by remember { mutableStateOf<String?>(null) }
                    var pendingVoiceNotePath by remember { mutableStateOf<String?>(null) }
                    val voiceNotePicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        pendingVoiceNotePath = uri?.toString()
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

                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { isDocketSheetOpen = true },
                                containerColor = Color(0xFFE67E22),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (pendingCount > 0) {
                                            Badge {
                                                Text(text = pendingCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Gavel, contentDescription = "Today's Docket")
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
                                                        Badge { Text(text = item.badgeCount.toString()) }
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
                            onAddVoiceNote = { voiceNotePicker.launch("audio/*") },
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

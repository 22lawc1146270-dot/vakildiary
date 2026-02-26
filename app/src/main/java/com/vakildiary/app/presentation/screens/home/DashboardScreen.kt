package com.vakildiary.app.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.presentation.components.AppCard
import com.vakildiary.app.presentation.components.ButtonLabel
import com.vakildiary.app.presentation.components.UrgencyBadge
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.viewmodels.DashboardViewModel
import com.vakildiary.app.presentation.viewmodels.SettingsViewModel
import com.vakildiary.app.presentation.viewmodels.state.DashboardUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onOpenOverdue: () -> Unit = {},
    onOpenDocket: () -> Unit = {},
    onAddTask: () -> Unit = {},
    docketPendingCount: Int = 0,
    viewModel: DashboardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val advocateName by settingsViewModel.advocateName.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VakilTheme.colors.bgPrimary
    ) {
        when (val state = uiState) {
            DashboardUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VakilTheme.colors.accentPrimary)
                }
            }
            is DashboardUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = VakilTheme.colors.error)
                }
            }
            is DashboardUiState.Success -> {
                DashboardContent(
                    advocateName = advocateName,
                    state = state,
                    onOpenOverdue = onOpenOverdue,
                    onOpenDocket = onOpenDocket,
                    onAddTask = onAddTask,
                    docketPendingCount = docketPendingCount
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    advocateName: String?,
    state: DashboardUiState.Success,
    onOpenOverdue: () -> Unit,
    onOpenDocket: () -> Unit,
    onAddTask: () -> Unit,
    docketPendingCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VakilTheme.spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            GreetingHeader(advocateName)
            
            IconButton(
                onClick = onOpenDocket,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = VakilTheme.colors.bgElevated
                )
            ) {
                BadgedBox(
                    badge = {
                        if (docketPendingCount > 0) {
                            Badge(containerColor = VakilTheme.colors.error) {
                                Text(text = docketPendingCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Today's Docket",
                        tint = VakilTheme.colors.accentPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(VakilTheme.spacing.lg))

        // Primary Card: Next/Today's Hearing
        FeaturedHearingCard(state.hearingsToday)

        Spacer(modifier = Modifier.height(VakilTheme.spacing.lg))

        // Horizontal Stats
        StatCardsRow(state)

        Spacer(modifier = Modifier.height(VakilTheme.spacing.lg))

        // Overdue Section
        if (state.overdueCount > 0) {
            OverdueCard(state.overdueCount, onOpenOverdue)
            Spacer(modifier = Modifier.height(VakilTheme.spacing.lg))
        }

        // Today's Tasks
        TodayTasksSection(state.tasksToday, onAddTask)
        
        Spacer(modifier = Modifier.height(VakilTheme.spacing.xl))
    }
}

@Composable
private fun GreetingHeader(advocateName: String?) {
    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    Column {
        Text(
            text = dateStr,
            style = VakilTheme.typography.headlineLarge,
            color = VakilTheme.colors.textPrimary
        )
        Text(
            text = if (advocateName.isNullOrBlank()) "Welcome back" else "Welcome, Adv. $advocateName",
            style = VakilTheme.typography.bodyLarge,
            color = VakilTheme.colors.textSecondary
        )
    }
}

@Composable
private fun FeaturedHearingCard(hearingsToday: List<String>) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(VakilTheme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Hearings",
                    style = VakilTheme.typography.labelMedium,
                    color = VakilTheme.colors.accentPrimary
                )
                if (hearingsToday.isNotEmpty()) {
                    UrgencyBadge(daysRemaining = 0)
                }
            }
            
            Spacer(modifier = Modifier.height(VakilTheme.spacing.sm))

            if (hearingsToday.isEmpty()) {
                Text(
                    text = "No hearings scheduled for today",
                    style = VakilTheme.typography.headlineMedium,
                    color = VakilTheme.colors.textPrimary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.xs)) {
                    hearingsToday.forEach { hearing ->
                        Text(
                            text = hearing,
                            style = VakilTheme.typography.headlineMedium,
                            color = VakilTheme.colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCardsRow(state: DashboardUiState.Success) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md),
        contentPadding = PaddingValues(end = VakilTheme.spacing.md)
    ) {
        item {
            StatCard("Active Cases", state.totalCases.toString())
        }
        item {
            StatCard("This Week", state.upcomingIn7Days.toString(), "Hearings")
        }
        item {
            StatCard("Pending Fees", state.pendingFees)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, subLabel: String? = null) {
    AppCard(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VakilTheme.spacing.md),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = VakilTheme.typography.headlineMedium,
                color = VakilTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = VakilTheme.typography.labelSmall,
                color = VakilTheme.colors.textSecondary
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    style = VakilTheme.typography.labelSmall,
                    color = VakilTheme.colors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun OverdueCard(count: Int, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VakilTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Overdue Tasks",
                    style = VakilTheme.typography.headlineMedium,
                    color = VakilTheme.colors.error
                )
                Text(
                    text = "Requires immediate attention",
                    style = VakilTheme.typography.labelSmall,
                    color = VakilTheme.colors.textSecondary
                )
            }
            UrgencyBadge(daysRemaining = -1)
        }
    }
}

@Composable
private fun TodayTasksSection(tasksToday: List<String>, onAddTask: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = VakilTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Today's Tasks",
            style = VakilTheme.typography.labelMedium,
            color = VakilTheme.colors.textSecondary
        )
        TextButton(onClick = onAddTask) {
            ButtonLabel(text = "Add Task")
        }
    }
    
    if (tasksToday.isEmpty()) {
        Text(
            text = "Clean slate for today",
            style = VakilTheme.typography.bodyLarge,
            color = VakilTheme.colors.textTertiary
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)) {
            tasksToday.forEach { task ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = task,
                        style = VakilTheme.typography.bodyLarge,
                        modifier = Modifier.padding(VakilTheme.spacing.md),
                        color = VakilTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

package com.vakildiary.app.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.presentation.viewmodels.DashboardViewModel
import com.vakildiary.app.presentation.viewmodels.state.DashboardUiState

@Composable
fun DashboardScreen(
    onOpenOverdue: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        DashboardUiState.Loading -> {
            Text(text = "Loading...", modifier = Modifier.padding(16.dp))
        }
        is DashboardUiState.Error -> {
            Text(text = state.message, modifier = Modifier.padding(16.dp))
        }
        is DashboardUiState.Success -> {
            DashboardContent(
                hearingsToday = state.hearingsToday,
                tasksToday = state.tasksToday,
                overdueCount = state.overdueCount,
                totalCases = state.totalCases,
                pendingFees = state.pendingFees,
                upcomingIn7Days = state.upcomingIn7Days,
                onOpenOverdue = onOpenOverdue
            )
        }
    }
}

@Composable
private fun DashboardContent(
    hearingsToday: List<String>,
    tasksToday: List<String>,
    overdueCount: Int,
    totalCases: Int,
    pendingFees: String,
    upcomingIn7Days: Int,
    onOpenOverdue: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Dashboard", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Today's Hearings", style = MaterialTheme.typography.titleMedium)
                if (hearingsToday.isEmpty()) {
                    Text(text = "No hearings today", style = MaterialTheme.typography.bodyMedium)
                } else {
                    hearingsToday.forEach { item ->
                        Text(text = "• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Today's Tasks", style = MaterialTheme.typography.titleMedium)
                if (tasksToday.isEmpty()) {
                    Text(text = "No tasks due today", style = MaterialTheme.typography.bodyMedium)
                } else {
                    tasksToday.forEach { item ->
                        Text(text = "• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenOverdue)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Overdue", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = overdueCount.toString(),
                    color = Color(0xFFC0392B),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = "Total cases", value = totalCases.toString())
                StatItem(label = "Pending fees", value = pendingFees)
                StatItem(label = "Upcoming in 7 days", value = upcomingIn7Days.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

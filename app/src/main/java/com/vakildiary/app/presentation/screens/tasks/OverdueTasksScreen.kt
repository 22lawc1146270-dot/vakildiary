package com.vakildiary.app.presentation.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.Task
import com.vakildiary.app.presentation.viewmodels.OverdueTasksViewModel
import com.vakildiary.app.presentation.viewmodels.state.OverdueTasksUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OverdueTasksScreen(
    onBack: () -> Unit,
    viewModel: OverdueTasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Overdue Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            OverdueTasksUiState.Loading -> {
                Text(text = "Loading...", modifier = Modifier.padding(16.dp))
            }
            is OverdueTasksUiState.Error -> {
                Text(text = state.message, modifier = Modifier.padding(16.dp))
            }
            is OverdueTasksUiState.Success -> {
                if (state.tasks.isEmpty()) {
                    Text(text = "No overdue tasks", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.tasks, key = { it.taskId }) { task ->
                            OverdueTaskRow(task = task, onMarkDone = { viewModel.markComplete(task.taskId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueTaskRow(task: Task, onMarkDone: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Case: ${task.caseId}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Due: ${formatDate(task.deadline)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onMarkDone) {
                    Text(text = "Mark Done")
                }
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

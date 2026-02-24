package com.vakildiary.app.presentation.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.domain.model.Task
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.viewmodels.TaskListViewModel
import com.vakildiary.app.presentation.viewmodels.state.TaskListUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TaskListScreen(
    caseId: String,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState(caseId).collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Completed", "Overdue")

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        when (val state = uiState) {
            TaskListUiState.Loading -> Text(text = "Loading...")
            is TaskListUiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error)
            is TaskListUiState.Success -> {
                val list = when (selectedTab) {
                    0 -> state.pending
                    1 -> state.completed
                    else -> state.overdue
                }
                if (list.isEmpty()) {
                    Text(text = "No tasks yet", modifier = Modifier.padding(8.dp))
                } else {
                    TaskList(
                        tasks = list,
                        onToggleComplete = { task, isComplete ->
                            viewModel.toggleComplete(task.taskId, isComplete)
                        },
                        onDelete = { task ->
                            viewModel.deleteTask(task.taskId)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskList(
    tasks: List<Task>,
    onToggleComplete: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tasks, key = { it.taskId }) { task ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    when (value) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            onToggleComplete(task, !task.isCompleted)
                            true
                        }
                        SwipeToDismissBoxValue.EndToStart -> {
                            onDelete(task)
                            true
                        }
                        else -> false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val direction = dismissState.dismissDirection
                    val (icon, tint, label) = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            if (task.isCompleted) {
                                Triple(Icons.Default.Check, VakilTheme.colors.success, "Reopen")
                            } else {
                                Triple(Icons.Default.Check, VakilTheme.colors.success, "Complete")
                            }
                        }
                        SwipeToDismissBoxValue.EndToStart -> Triple(Icons.Default.Delete, VakilTheme.colors.error, "Delete")
                        else -> Triple(Icons.Default.Check, Color.Transparent, "")
                    }
                    SwipeBackground(icon = icon, tint = tint, label = label)
                }
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${taskTypeLabel(task.taskType)} • ${formatDate(task.deadline)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (!task.isCompleted && task.deadline < System.currentTimeMillis()) {
                            Text(text = "Overdue", color = VakilTheme.colors.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeBackground(icon: ImageVector, tint: Color, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row {
                if (label.isNotBlank()) {
                    Icon(imageVector = icon, contentDescription = label, tint = tint)
                    Text(
                        text = label,
                        color = tint,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun taskTypeLabel(type: com.vakildiary.app.domain.model.TaskType): String {
    return when (type) {
        com.vakildiary.app.domain.model.TaskType.FILE_PETITION -> "File Petition"
        com.vakildiary.app.domain.model.TaskType.COLLECT_PAPERS -> "Collect Papers from Court"
        com.vakildiary.app.domain.model.TaskType.VIEW_ORDERSHEET -> "View Ordersheet"
        com.vakildiary.app.domain.model.TaskType.PHOTOCOPY -> "Get Photocopies"
        com.vakildiary.app.domain.model.TaskType.PAY_COURT_FEES -> "Pay Court Fees"
        com.vakildiary.app.domain.model.TaskType.PREPARE_ARGUMENTS -> "Prepare Arguments"
        com.vakildiary.app.domain.model.TaskType.MEETING -> "Meeting"
        com.vakildiary.app.domain.model.TaskType.CUSTOM -> "Custom"
    }
}

private fun formatDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

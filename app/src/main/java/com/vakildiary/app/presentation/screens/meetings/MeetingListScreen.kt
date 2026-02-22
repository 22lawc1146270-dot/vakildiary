package com.vakildiary.app.presentation.screens.meetings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vakildiary.app.domain.model.Meeting
import com.vakildiary.app.presentation.viewmodels.MeetingViewModel
import com.vakildiary.app.presentation.viewmodels.state.MeetingUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MeetingListScreen(
    caseId: String,
    showTopBar: Boolean = false,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: MeetingViewModel = hiltViewModel()
) {
    val uiState by viewModel.meetings(caseId).collectAsState()

    val content: @Composable (Modifier) -> Unit = { modifier ->
        when (uiState) {
            MeetingUiState.Loading -> {
                Text(text = "Loading...", modifier = modifier.padding(16.dp))
            }
            is MeetingUiState.Error -> {
                Text(
                    text = (uiState as MeetingUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = modifier.padding(16.dp)
                )
            }
            is MeetingUiState.Success -> {
                val meetings = (uiState as MeetingUiState.Success).meetings
                LazyColumn(
                    modifier = modifier,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(meetings, key = { it.meetingId }) { meeting ->
                        MeetingCard(meeting = meeting)
                    }
                }
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Meetings") },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = onBack) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            content(Modifier.padding(paddingValues))
        }
    } else {
        content(Modifier)
    }
}

@Composable
private fun MeetingCard(meeting: Meeting) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = meeting.clientName, style = MaterialTheme.typography.titleMedium)
            Text(text = "${formatDate(meeting.meetingDate)} • ${meeting.location}")
            Text(text = meeting.agenda, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a"))
}

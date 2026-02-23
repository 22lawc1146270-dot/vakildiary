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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vakildiary.app.presentation.viewmodels.UpcomingMeetingItem
import com.vakildiary.app.presentation.viewmodels.UpcomingMeetingsViewModel
import com.vakildiary.app.presentation.viewmodels.state.UpcomingMeetingsUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingMeetingsScreen(
    onBack: () -> Unit,
    viewModel: UpcomingMeetingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Upcoming Meetings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            UpcomingMeetingsUiState.Loading -> {
                Text(text = "Loading...", modifier = Modifier.padding(16.dp))
            }
            is UpcomingMeetingsUiState.Error -> {
                Text(
                    text = (uiState as UpcomingMeetingsUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is UpcomingMeetingsUiState.Success -> {
                val meetings = (uiState as UpcomingMeetingsUiState.Success).meetings
                if (meetings.isEmpty()) {
                    Text(text = "No upcoming meetings", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(meetings, key = { it.meeting.meetingId }) { item ->
                            UpcomingMeetingCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingMeetingCard(item: UpcomingMeetingItem) {
    val meeting = item.meeting
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = meeting.clientName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Case: ${item.caseName ?: meeting.caseId}")
            Text(text = "${formatDate(meeting.meetingDate)} • ${meeting.location}")
            if (meeting.agenda.isNotBlank()) {
                Text(text = meeting.agenda, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a"))
}

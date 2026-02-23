package com.vakildiary.app.presentation.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.vakildiary.app.presentation.viewmodels.CalendarViewModel
import com.vakildiary.app.presentation.viewmodels.TodayDocketViewModel
import com.vakildiary.app.presentation.screens.docket.HearingOutcomeDialog
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import android.net.Uri

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var isWeekView by remember { mutableStateOf(false) }
    val events by viewModel.events.collectAsStateWithLifecycle()
    val docketViewModel: TodayDocketViewModel = hiltViewModel()
    val context = LocalContext.current
    var pendingOutcomeHearingId by remember { mutableStateOf<String?>(null) }
    var pendingOutcomeCaseName by remember { mutableStateOf<String?>(null) }
    var pendingVoiceNotePath by remember { mutableStateOf<String?>(null) }
    val voiceNoteRecorder = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        pendingVoiceNotePath = uri?.let { copyVoiceNoteToInternal(context, it) }
    }

    val month = remember(selectedDate) { YearMonth.from(selectedDate) }
    val monthDays = remember(month) { buildMonthGrid(month) }
    val eventsByDate = remember(events) { events.groupBy { it.date } }

    LaunchedEffect(month) {
        viewModel.loadMonth(month.atDay(1))
    }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !isWeekView,
                onClick = { isWeekView = false },
                label = { Text(text = "Month") }
            )
            FilterChip(
                selected = isWeekView,
                onClick = { isWeekView = true },
                label = { Text(text = "Week") }
            )
        }

        DayOfWeekHeader()

        if (isWeekView) {
            WeekRow(
                selectedDate = selectedDate,
                eventsByDate = eventsByDate,
                onDateSelected = { selectedDate = it }
            )
        } else {
            MonthGrid(
                days = monthDays,
                selectedDate = selectedDate,
                eventsByDate = eventsByDate,
                onDateSelected = { selectedDate = it }
            )
        }

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(animationSpec = tween(250), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = tween(200), targetOffsetY = { it })
        ) {
            DayAgendaPanel(
                date = selectedDate,
                events = eventsByDate[selectedDate].orEmpty(),
                onEventClick = { event ->
                    if (event.type == CalendarEventType.HEARING) {
                        pendingOutcomeHearingId = event.id
                        pendingOutcomeCaseName = event.title
                        pendingVoiceNotePath = null
                    }
                }
            )
        }
    }

    if (pendingOutcomeHearingId != null) {
        HearingOutcomeDialog(
            caseName = pendingOutcomeCaseName ?: "Case",
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
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            Text(text = day, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MonthGrid(
    days: List<LocalDate?>,
    selectedDate: LocalDate,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        events = date?.let { eventsByDate[it].orEmpty() }.orEmpty(),
                        onClick = { if (date != null) onDateSelected(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekRow(
    selectedDate: LocalDate,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val startOfWeek = selectedDate.minusDays(((selectedDate.dayOfWeek.value + 6) % 7).toLong())
    val week = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { date ->
            DayCell(
                date = date,
                isSelected = date == selectedDate,
                events = eventsByDate[date].orEmpty(),
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(42.dp)
            .border(1.dp, borderColor)
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = date.dayOfMonth.toString(), color = textColor, style = MaterialTheme.typography.labelMedium)
                if (events.isNotEmpty()) {
                    DotRow(events = events)
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DotRow(events: List<CalendarEvent>) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
        events.take(3).forEach { event ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(event.type.color, shape = MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
private fun DayAgendaPanel(
    date: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium
            )

            if (events.isEmpty()) {
                Text(text = "No hearings or tasks on this date", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(events) { event ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable(enabled = event.type == CalendarEventType.HEARING) {
                                onEventClick(event)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(event.type.color, shape = MaterialTheme.shapes.small)
                            )
                            Column {
                                Text(text = event.title, style = MaterialTheme.typography.bodyMedium)
                                Text(text = event.subtitle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildMonthGrid(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startIndex = (first.dayOfWeek.value + 6) % 7

    val result = mutableListOf<LocalDate?>()
    repeat(startIndex) { result.add(null) }
    for (day in 1..daysInMonth) {
        result.add(month.atDay(day))
    }
    while (result.size % 7 != 0) {
        result.add(null)
    }
    return result
}

private fun copyVoiceNoteToInternal(context: android.content.Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "voice_notes")
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    } catch (t: Throwable) {
        null
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

data class CalendarEvent(
    val id: String,
    val date: LocalDate,
    val title: String,
    val subtitle: String,
    val type: CalendarEventType
)

enum class CalendarEventType(val color: Color) {
    HEARING(Color(0xFF2E75B6)),
    TASK(Color(0xFFE67E22)),
    MEETING(Color(0xFF7E57C2))
}

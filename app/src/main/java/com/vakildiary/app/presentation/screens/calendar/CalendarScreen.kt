package com.vakildiary.app.presentation.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.vakildiary.app.presentation.components.ButtonLabel
import com.vakildiary.app.presentation.viewmodels.CalendarViewModel
import com.vakildiary.app.presentation.viewmodels.TodayDocketViewModel
import com.vakildiary.app.presentation.screens.docket.HearingOutcomeDialog
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.components.AppCard
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddTask: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VakilTheme.colors.bgPrimary
    ) {
        Column(
            modifier = Modifier.padding(VakilTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = VakilTheme.typography.headlineMedium,
                    color = VakilTheme.colors.textPrimary
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.xs)) {
                    ViewToggleChip(selected = !isWeekView, onClick = { isWeekView = false }, label = "Month")
                    ViewToggleChip(selected = isWeekView, onClick = { isWeekView = true }, label = "Week")
                }
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

            Spacer(modifier = Modifier.height(VakilTheme.spacing.sm))

            DayAgendaPanel(
                date = selectedDate,
                events = eventsByDate[selectedDate].orEmpty(),
                onAddTask = onAddTask,
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
private fun ViewToggleChip(selected: Boolean, onClick: () -> Unit, label: String) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) VakilTheme.colors.accentPrimary else VakilTheme.colors.bgElevated,
        contentColor = if (selected) VakilTheme.colors.onAccent else VakilTheme.colors.textSecondary
    ) {
        Text(
            text = label,
            style = VakilTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        days.forEach { day ->
            Text(
                text = day,
                style = VakilTheme.typography.labelSmall,
                color = VakilTheme.colors.textTertiary,
                modifier = Modifier.width(42.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
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
    Column(verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)) {
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
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
    val isToday = date == LocalDate.now()
    
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = if (isSelected) VakilTheme.colors.accentPrimary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = when {
                        isSelected -> VakilTheme.colors.onAccent
                        isToday -> VakilTheme.colors.accentPrimary
                        else -> VakilTheme.colors.textPrimary
                    },
                    style = VakilTheme.typography.labelMedium,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (events.isNotEmpty()) {
                    DotRow(events = events, isSelected = isSelected)
                }
            }
        }
    }
}

@Composable
private fun DotRow(events: List<CalendarEvent>, isSelected: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
        events.take(3).forEach { event ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        if (isSelected) VakilTheme.colors.onAccent else event.type.color,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun DayAgendaPanel(
    date: LocalDate,
    events: List<CalendarEvent>,
    onAddTask: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = VakilTheme.typography.labelMedium,
                color = VakilTheme.colors.accentPrimary
            )
            TextButton(onClick = onAddTask) {
                ButtonLabel(text = "Add Task")
            }
        }

        if (events.isEmpty()) {
            Text(
                text = "No hearings or tasks on this date",
                style = VakilTheme.typography.bodyLarge,
                color = VakilTheme.colors.textTertiary
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm),
                modifier = Modifier.fillMaxHeight()
            ) {
                items(events) { event ->
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = event.type == CalendarEventType.HEARING) {
                                onEventClick(event)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(VakilTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(event.type.color, shape = CircleShape)
                            )
                            Column {
                                Text(
                                    text = event.title,
                                    style = VakilTheme.typography.bodyLarge,
                                    color = VakilTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = event.subtitle,
                                    style = VakilTheme.typography.labelSmall,
                                    color = VakilTheme.colors.textSecondary
                                )
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

enum class CalendarEventType {
    HEARING,
    TASK,
    MEETING
}

val CalendarEventType.color: Color
    @Composable
    get() = when (this) {
        CalendarEventType.HEARING -> VakilTheme.colors.accentPrimary
        CalendarEventType.TASK -> VakilTheme.colors.warning
        CalendarEventType.MEETING -> VakilTheme.colors.info
    }

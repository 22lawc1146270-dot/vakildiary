package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.dao.MeetingDao
import com.vakildiary.app.data.local.dao.TaskDao
import com.vakildiary.app.presentation.screens.calendar.CalendarEvent
import com.vakildiary.app.presentation.screens.calendar.CalendarEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val caseDao: CaseDao,
    private val taskDao: TaskDao,
    private val meetingDao: MeetingDao
) : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    fun loadMonth(month: LocalDate) {
        val start = month.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = month.withDayOfMonth(month.lengthOfMonth())
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val hearings = caseDao.getCasesWithHearingsBetween(start, end).mapNotNull { entity ->
                val date = entity.nextHearingDate ?: return@mapNotNull null
                val localDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
                CalendarEvent(
                    id = entity.caseId,
                    date = localDate,
                    title = entity.caseName,
                    subtitle = entity.courtName,
                    type = CalendarEventType.HEARING
                )
            }

            val tasks = taskDao.getTasksBetween(start, end).map { entity ->
                val localDate = Instant.ofEpochMilli(entity.deadline).atZone(ZoneId.systemDefault()).toLocalDate()
                CalendarEvent(
                    id = entity.taskId,
                    date = localDate,
                    title = entity.title,
                    subtitle = "Task",
                    type = CalendarEventType.TASK
                )
            }

            val meetings = meetingDao.getMeetingsBetween(start, end).map { entity ->
                val localDate = Instant.ofEpochMilli(entity.meetingDate).atZone(ZoneId.systemDefault()).toLocalDate()
                CalendarEvent(
                    id = entity.meetingId,
                    date = localDate,
                    title = entity.clientName,
                    subtitle = "Meeting",
                    type = CalendarEventType.MEETING
                )
            }

            _events.value = hearings + tasks + meetings
        }
    }
}

package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.dao.PaymentDao
import com.vakildiary.app.data.local.dao.TaskDao
import com.vakildiary.app.presentation.viewmodels.state.DashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val caseDao: CaseDao,
    private val taskDao: TaskDao,
    private val paymentDao: PaymentDao
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        caseDao.getAllActiveCases(),
        caseDao.getCasesWithHearingToday(),
        taskDao.getTasksDueToday(),
        taskDao.getOverdueTasks(System.currentTimeMillis()),
        paymentDao.getAllPayments()
    ) { cases, hearingsToday, tasksToday, overdueTasks, payments ->
        val caseNameMap = cases.associateBy({ it.caseId }, { it.caseName })
        val hearingItems = hearingsToday.map { caseEntity ->
            val time = caseEntity.nextHearingDate?.let { formatTime(it) }
            if (time.isNullOrBlank()) caseEntity.caseName else "${caseEntity.caseName} • $time"
        }
        val taskItems = tasksToday.map { task ->
            val caseName = caseNameMap[task.caseId] ?: task.caseId
            "${task.title} • $caseName"
        }
        val totalCases = cases.size
        val overdueCount = overdueTasks.size
        val upcomingIn7Days = cases.count { caseEntity ->
            val next = caseEntity.nextHearingDate ?: return@count false
            val now = System.currentTimeMillis()
            next in now..(now + DAYS_7_MILLIS)
        }

        val paymentsByCase = payments.groupBy { it.caseId }
        val pendingFees = cases.sumOf { caseEntity ->
            val agreed = caseEntity.totalAgreedFees ?: 0.0
            val received = paymentsByCase[caseEntity.caseId]?.sumOf { it.amount } ?: 0.0
            (agreed - received).coerceAtLeast(0.0)
        }

        DashboardUiState.Success(
            hearingsToday = hearingItems,
            tasksToday = taskItems,
            overdueCount = overdueCount,
            totalCases = totalCases,
            pendingFees = "₹${pendingFees.toInt()}",
            upcomingIn7Days = upcomingIn7Days
        )
    }
        .catch { emit(DashboardUiState.Error("Failed to load dashboard")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    private fun formatTime(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    companion object {
        private const val DAYS_7_MILLIS = 7 * 24 * 60 * 60 * 1000L
    }
}

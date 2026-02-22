package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.dao.HearingHistoryDao
import com.vakildiary.app.data.local.dao.TaskDao
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.domain.usecase.cases.GetCaseByIdUseCase
import com.vakildiary.app.domain.usecase.cases.UpdateCaseUseCase
import com.vakildiary.app.domain.usecase.hearing.AddHearingUseCase
import com.vakildiary.app.domain.usecase.task.MarkTaskCompleteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TodayDocketViewModel @Inject constructor(
    private val caseDao: CaseDao,
    private val taskDao: TaskDao,
    private val hearingHistoryDao: HearingHistoryDao,
    private val addHearingUseCase: AddHearingUseCase,
    private val getCaseByIdUseCase: GetCaseByIdUseCase,
    private val updateCaseUseCase: UpdateCaseUseCase,
    private val markTaskCompleteUseCase: MarkTaskCompleteUseCase
) : ViewModel() {

    val uiState: StateFlow<DocketUiState> = combine(
        caseDao.getCasesWithHearingToday(),
        taskDao.getTasksDueToday(),
        hearingHistoryDao.getCaseIdsWithHearingToday()
    ) { cases, tasks, completedCaseIds ->
        val hearingItems = cases.map { caseEntity ->
            DocketItem(
                id = caseEntity.caseId,
                title = caseEntity.caseName,
                subtitle = buildHearingSubtitle(caseEntity.courtName, caseEntity.nextHearingDate),
                type = DocketType.HEARING,
                isCompleted = completedCaseIds.contains(caseEntity.caseId)
            )
        }

        val taskItems = tasks.map { taskEntity ->
            DocketItem(
                id = taskEntity.taskId,
                title = taskEntity.title,
                subtitle = "Case: ${taskEntity.caseId}",
                type = DocketType.TASK,
                isCompleted = taskEntity.isCompleted
            )
        }

        val completedCount = hearingItems.count { it.isCompleted } + taskItems.count { it.isCompleted }
        val totalCount = hearingItems.size + taskItems.size

        DocketUiState.Success(
            hearings = hearingItems,
            tasks = taskItems,
            completedCount = completedCount,
            totalCount = totalCount
        )
    }
        .catch { emit(DocketUiState.Error("Failed to load today's docket")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocketUiState.Loading)

    fun markHearingComplete(
        hearingId: String,
        outcome: String?,
        orderDetails: String?,
        adjournmentReason: String?,
        voiceNotePath: String?,
        nextDate: Long?
    ) {
        // hearingId represents the caseId for today's hearing item.
        val caseId = hearingId
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val hearing = HearingHistory(
                hearingId = UUID.randomUUID().toString(),
                caseId = caseId,
                hearingDate = now,
                purpose = "Hearing",
                outcome = outcome,
                orderDetails = orderDetails,
                nextDateGiven = nextDate,
                adjournmentReason = adjournmentReason,
                voiceNotePath = voiceNotePath,
                createdAt = now
            )

            when (val addResult = addHearingUseCase(hearing)) {
                is Result.Error -> return@launch
                is Result.Success -> {
                    if (nextDate != null) {
                        when (val caseResult = getCaseByIdUseCase(caseId).first()) {
                            is Result.Success -> {
                                val case = caseResult.data ?: return@launch
                                updateCaseUseCase(case.copy(nextHearingDate = nextDate))
                            }
                            is Result.Error -> return@launch
                        }
                    }
                }
            }
        }
    }

    fun getCaseName(caseId: String): String? {
        return runCatching {
            // This is a best-effort non-suspending lookup from cached UI state.
            (uiState.value as? DocketUiState.Success)
                ?.hearings
                ?.firstOrNull { it.id == caseId }
                ?.title
        }.getOrNull()
    }

    fun markTaskComplete(taskId: String) {
        viewModelScope.launch {
            markTaskCompleteUseCase(taskId, true)
        }
    }

    private fun buildHearingSubtitle(courtName: String, nextHearingDate: Long?): String {
        val time = nextHearingDate?.let { formatTime(it) }
        return if (time.isNullOrBlank()) courtName else "$courtName • $time"
    }

    private fun formatTime(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }
}

sealed interface DocketUiState {
    object Loading : DocketUiState
    data class Success(
        val hearings: List<DocketItem>,
        val tasks: List<DocketItem>,
        val completedCount: Int,
        val totalCount: Int
    ) : DocketUiState
    data class Error(val message: String) : DocketUiState
}

data class DocketItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: DocketType,
    val isCompleted: Boolean
)

enum class DocketType {
    HEARING,
    TASK
}

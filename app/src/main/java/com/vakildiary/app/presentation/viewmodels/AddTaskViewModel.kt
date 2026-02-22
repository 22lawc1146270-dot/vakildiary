package com.vakildiary.app.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Task
import com.vakildiary.app.domain.model.TaskType
import com.vakildiary.app.domain.usecase.cases.GetAllCasesUseCase
import com.vakildiary.app.domain.usecase.task.AddTaskUseCase
import com.vakildiary.app.presentation.viewmodels.state.AddTaskUiState
import com.vakildiary.app.presentation.viewmodels.state.CasePickerUiState
import com.vakildiary.app.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val addTaskUseCase: AddTaskUseCase,
    private val getAllCasesUseCase: GetAllCasesUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddTaskUiState>(AddTaskUiState.Success(false))
    val uiState: StateFlow<AddTaskUiState> = _uiState.asStateFlow()

    val casesState: StateFlow<CasePickerUiState> = getAllCasesUseCase()
        .map { result ->
            when (result) {
                is Result.Success -> CasePickerUiState.Success(result.data)
                is Result.Error -> CasePickerUiState.Error(result.message)
            }
        }
        .catch { emit(CasePickerUiState.Error("Failed to load cases")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CasePickerUiState.Loading)

    fun saveTask(
        caseId: String,
        title: String,
        taskType: TaskType,
        deadlineMillis: Long,
        reminderMinutesBefore: Int
    ) {
        viewModelScope.launch {
            _uiState.value = AddTaskUiState.Loading
            val taskId = UUID.randomUUID().toString()
            val task = Task(
                taskId = taskId,
                caseId = caseId,
                title = title,
                taskType = taskType,
                deadline = deadlineMillis,
                reminderMinutesBefore = reminderMinutesBefore,
                isCompleted = false,
                completedAt = null,
                notes = null,
                createdAt = System.currentTimeMillis()
            )
            _uiState.value = when (val result = addTaskUseCase(task)) {
                is Result.Success -> {
                    scheduleReminderIfNeeded(taskId, deadlineMillis, reminderMinutesBefore)
                    AddTaskUiState.Success(true)
                }
                is Result.Error -> AddTaskUiState.Error(result.message)
            }
        }
    }

    private fun scheduleReminderIfNeeded(taskId: String, deadlineMillis: Long, reminderMinutesBefore: Int) {
        if (reminderMinutesBefore <= 0) return
        val triggerAt = deadlineMillis - reminderMinutesBefore * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return
        NotificationScheduler.scheduleTaskReminder(context, taskId, triggerAt)
    }
}

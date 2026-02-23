package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.usecase.task.GetCompletedTasksUseCase
import com.vakildiary.app.domain.usecase.task.GetPendingTasksUseCase
import com.vakildiary.app.domain.usecase.task.GetOverdueTasksByCaseIdUseCase
import com.vakildiary.app.domain.usecase.task.MarkTaskCompleteUseCase
import com.vakildiary.app.domain.usecase.task.DeleteTaskUseCase
import com.vakildiary.app.presentation.viewmodels.state.TaskListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getPendingTasksUseCase: GetPendingTasksUseCase,
    private val getCompletedTasksUseCase: GetCompletedTasksUseCase,
    private val getOverdueTasksByCaseIdUseCase: GetOverdueTasksByCaseIdUseCase,
    private val markTaskCompleteUseCase: MarkTaskCompleteUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    fun uiState(caseId: String): StateFlow<TaskListUiState> {
        return combine(
            getPendingTasksUseCase(caseId),
            getCompletedTasksUseCase(caseId),
            getOverdueTasksByCaseIdUseCase(caseId)
        ) { pendingResult, completedResult, overdueResult ->
            val pending = (pendingResult as? Result.Success)?.data.orEmpty()
            val completed = (completedResult as? Result.Success)?.data.orEmpty()
            val overdue = (overdueResult as? Result.Success)?.data.orEmpty()

            TaskListUiState.Success(
                pending = pending,
                completed = completed,
                overdue = overdue
            ) as TaskListUiState
        }
            .catch { emit(TaskListUiState.Error("Failed to load tasks")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState.Loading)
    }

    fun toggleComplete(taskId: String, isComplete: Boolean) {
        viewModelScope.launch {
            markTaskCompleteUseCase(taskId, isComplete)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
        }
    }
}

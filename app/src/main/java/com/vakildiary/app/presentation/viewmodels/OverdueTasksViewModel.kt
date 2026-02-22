package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.usecase.task.GetOverdueTasksUseCase
import com.vakildiary.app.domain.usecase.task.MarkTaskCompleteUseCase
import com.vakildiary.app.presentation.viewmodels.state.OverdueTasksUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OverdueTasksViewModel @Inject constructor(
    private val getOverdueTasksUseCase: GetOverdueTasksUseCase,
    private val markTaskCompleteUseCase: MarkTaskCompleteUseCase
) : ViewModel() {
    val overdueCount: StateFlow<Int> = getOverdueTasksUseCase()
        .map { result -> (result as? Result.Success)?.data?.size ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val uiState: StateFlow<OverdueTasksUiState> = getOverdueTasksUseCase()
        .map { result ->
            when (result) {
                is Result.Success -> OverdueTasksUiState.Success(result.data)
                is Result.Error -> OverdueTasksUiState.Error(result.message)
            }
        }
        .catch { emit(OverdueTasksUiState.Error("Failed to load overdue tasks")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverdueTasksUiState.Loading)

    fun markComplete(taskId: String) {
        viewModelScope.launch {
            markTaskCompleteUseCase(taskId, true)
        }
    }
}

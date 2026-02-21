package com.vakildiary.app.domain.usecase.task

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MarkTaskCompleteUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, isComplete: Boolean): Result<Unit> {
        return when (val result = taskRepository.getTaskById(taskId).first()) {
            is Result.Success -> {
                val task = result.data.copy(
                    isCompleted = isComplete,
                    completedAt = if (isComplete) System.currentTimeMillis() else null
                )
                taskRepository.updateTask(task)
            }

            is Result.Error -> result
        }
    }
}

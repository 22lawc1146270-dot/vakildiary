package com.vakildiary.app.domain.usecase.task

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return when (val result = taskRepository.getTaskById(taskId).first()) {
            is Result.Success -> taskRepository.deleteTask(result.data)
            is Result.Error -> result
        }
    }
}

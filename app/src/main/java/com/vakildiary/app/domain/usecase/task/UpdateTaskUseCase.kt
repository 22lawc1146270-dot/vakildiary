package com.vakildiary.app.domain.usecase.task

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Task
import com.vakildiary.app.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Result<Unit> {
        return taskRepository.updateTask(task)
    }
}

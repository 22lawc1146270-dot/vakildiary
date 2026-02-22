package com.vakildiary.app.domain.usecase.task

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Task
import com.vakildiary.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCompletedTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(caseId: String): Flow<Result<List<Task>>> {
        return taskRepository.getCompletedTasksByCaseId(caseId)
    }
}

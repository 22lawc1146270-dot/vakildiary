package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun insertTask(task: Task): Result<Unit>
    suspend fun updateTask(task: Task): Result<Unit>
    fun getPendingTasksByCaseId(caseId: String): Flow<Result<List<Task>>>
    fun getCompletedTasksByCaseId(caseId: String): Flow<Result<List<Task>>>
    fun getOverdueTasks(now: Long): Flow<Result<List<Task>>>
    fun getOverdueTasksByCaseId(caseId: String, now: Long): Flow<Result<List<Task>>>
    fun getTaskById(id: String): Flow<Result<Task>>
    suspend fun deleteTask(task: Task): Result<Unit>
}

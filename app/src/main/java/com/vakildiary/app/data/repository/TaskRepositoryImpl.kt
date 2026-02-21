package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.local.dao.TaskDao
import com.vakildiary.app.data.local.entities.TaskEntity
import com.vakildiary.app.domain.model.Task
import com.vakildiary.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    override suspend fun insertTask(task: Task): Result<Unit> {
        return try {
            taskDao.insertTask(task.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to insert task", throwable = t)
        }
    }

    override suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            taskDao.updateTask(task.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to update task", throwable = t)
        }
    }

    override fun getPendingTasksByCaseId(caseId: String): Flow<Result<List<Task>>> {
        return taskDao.getPendingTasksByCaseId(caseId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override fun getCompletedTasksByCaseId(caseId: String): Flow<Result<List<Task>>> {
        return taskDao.getCompletedTasksByCaseId(caseId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override fun getOverdueTasks(now: Long): Flow<Result<List<Task>>> {
        return taskDao.getOverdueTasks(now).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override fun getTaskById(id: String): Flow<Result<Task>> {
        return taskDao.getTaskById(id).map { entity ->
            entity?.let { Result.Success(it.toDomainModel()) }
                ?: Result.Error("Task not found")
        }
    }

    override suspend fun deleteTask(task: Task): Result<Unit> {
        return try {
            taskDao.deleteTask(task.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to delete task", throwable = t)
        }
    }

    private fun Task.toEntity(): TaskEntity =
        TaskEntity(
            taskId = taskId,
            caseId = caseId,
            title = title,
            taskType = taskType,
            deadline = deadline,
            reminderMinutesBefore = reminderMinutesBefore,
            isCompleted = isCompleted,
            completedAt = completedAt,
            notes = notes,
            createdAt = createdAt
        )

    private fun TaskEntity.toDomain(): Task =
        Task(
            taskId = taskId,
            caseId = caseId,
            title = title,
            taskType = taskType,
            deadline = deadline,
            reminderMinutesBefore = reminderMinutesBefore,
            isCompleted = isCompleted,
            completedAt = completedAt,
            notes = notes,
            createdAt = createdAt
        )

    private fun TaskEntity.toDomainModel(): Task = toDomain()
}

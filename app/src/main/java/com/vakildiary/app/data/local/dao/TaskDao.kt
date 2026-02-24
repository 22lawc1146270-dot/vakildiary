package com.vakildiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vakildiary.app.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE caseId = :caseId AND isCompleted = 0 ORDER BY deadline ASC")
    fun getPendingTasksByCaseId(caseId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE caseId = :caseId AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasksByCaseId(caseId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE DATE(deadline/1000,'unixepoch') = DATE('now') AND isCompleted = 0")
    fun getTasksDueToday(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE DATE(deadline/1000,'unixepoch') = DATE('now')")
    fun getTasksDueTodayIncludingCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND deadline < :now ORDER BY deadline ASC")
    fun getOverdueTasks(now: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE caseId = :caseId AND isCompleted = 0 AND deadline < :now ORDER BY deadline ASC")
    fun getOverdueTasksByCaseId(caseId: String, now: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deadline BETWEEN :startMillis AND :endMillis")
    suspend fun getTasksBetween(startMillis: Long, endMillis: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE taskId = :id")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}

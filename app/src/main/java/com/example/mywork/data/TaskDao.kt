package com.example.mywork.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE workSessionId = :sessionId")
    fun getTasksForSession(sessionId: Long): Flow<List<Task>>

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT COUNT(*) FROM tasks WHERE workSessionId = :sessionId AND isCompleted = 1")
    fun getCompletedTasksCount(sessionId: Long): Flow<Int>

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
} 
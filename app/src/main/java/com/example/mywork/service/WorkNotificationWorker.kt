package com.example.mywork.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywork.data.WorkDatabase
import com.example.mywork.data.WorkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class WorkNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = WorkDatabase.getDatabase(context)
    private val repository = WorkRepository(
        workSessionDao = database.workSessionDao(),
        taskDao = database.taskDao()
    )
    private val notificationService = WorkNotificationService(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get the current session
            val now = LocalDateTime.now()
            val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
            val endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999)
            
            val sessions = repository.getSessionsForDay(startOfDay, endOfDay).first()
            val currentSession = sessions.find { it.exitTime == null }
            
            if (currentSession != null) {
                // Get tasks for the current session
                val tasks = repository.getTasksForSession(currentSession.id).first()
                
                // Count remaining tasks
                val remainingTasks = tasks.count { !it.isCompleted }
                
                // Calculate hour number (1-8)
                val startTime = currentSession.entryTime
                val now = LocalDateTime.now()
                val hoursElapsed = java.time.Duration.between(startTime, now).toHours()
                val hourNumber = (hoursElapsed + 1).toInt().coerceIn(1, 8)
                
                // Show notification
                notificationService.showHourlyNotification(hourNumber, remainingTasks)
                
                Result.success()
            } else {
                Result.success() // No active session, just return success
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
} 
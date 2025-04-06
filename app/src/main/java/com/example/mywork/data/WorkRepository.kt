package com.example.mywork.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration

class WorkRepository(
    private val workSessionDao: WorkSessionDao,
    private val taskDao: TaskDao
) {
    companion object {
        private const val DEFAULT_WORK_HOURS = 8L
        private const val DEFAULT_COMMUTE_TIME = 30 // Default commute time in minutes
        private val DEFAULT_START_TIME = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0)
        private val DEFAULT_END_TIME = DEFAULT_START_TIME.plusHours(DEFAULT_WORK_HOURS)
    }

    fun getAllSessions(): Flow<List<WorkSession>> = workSessionDao.getAllSessions()

    fun getSessionsForDay(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<List<WorkSession>> =
        workSessionDao.getSessionsForDay(startOfDay, endOfDay)

    fun getCurrentSession(): Flow<WorkSession?> {
        val now = LocalDateTime.now()
        val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999)
        
        return workSessionDao.getSessionsForDay(startOfDay, endOfDay).map { sessions ->
            sessions.find { it.exitTime == null }
        }
    }

    suspend fun startWorkSession(commuteTimeMinutes: Int = DEFAULT_COMMUTE_TIME) {
        val now = LocalDateTime.now()
        val startTime = if (now.isBefore(DEFAULT_START_TIME)) DEFAULT_START_TIME else now
        val endTime = startTime.plusHours(DEFAULT_WORK_HOURS)
        
        val session = WorkSession(
            entryTime = startTime,
            plannedExitTime = endTime,
            commuteTimeMinutes = commuteTimeMinutes,
            morningAlarmTime = startTime.minusMinutes(commuteTimeMinutes.toLong())
        )
        workSessionDao.insertSession(session)
    }

    suspend fun endWorkSession() {
        val now = LocalDateTime.now()
        val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999)
        
        val sessions = workSessionDao.getSessionsForDay(startOfDay, endOfDay).first()
        val currentSession = sessions.find { it.exitTime == null }
        
        currentSession?.let { session ->
            workSessionDao.updateSession(session.copy(exitTime = now))
        }
    }

    fun getTasksForSession(sessionId: Long): Flow<List<Task>> =
        taskDao.getTasksForSession(sessionId)

    suspend fun addTask(sessionId: Long, description: String) {
        val task = Task(
            description = description,
            isCompleted = false,
            workSessionId = sessionId
        )
        taskDao.insertTask(task)
    }

    suspend fun completeTask(task: Task) {
        taskDao.updateTask(task.copy(isCompleted = true))
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    fun getCompletedTasksCount(sessionId: Long): Flow<Int> =
        taskDao.getCompletedTasksCount(sessionId)

    fun getSession(sessionId: Long): Flow<WorkSession?> =
        workSessionDao.getSession(sessionId)

    suspend fun clearAllData() {
        workSessionDao.clearAllSessions()
        taskDao.clearAllTasks()
    }

    suspend fun updateSessionTimes(sessionId: Long, newStartTime: LocalDateTime) {
        val session = workSessionDao.getSession(sessionId).first()
        if (session != null) {
            val newEndTime = newStartTime.plusHours(DEFAULT_WORK_HOURS)
            workSessionDao.updateSession(session.copy(
                entryTime = newStartTime,
                plannedExitTime = newEndTime
            ))
        }
    }
}
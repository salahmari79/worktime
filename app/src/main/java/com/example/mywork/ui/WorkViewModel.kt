package com.example.mywork.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mywork.data.Task
import com.example.mywork.data.WorkRepository
import com.example.mywork.data.WorkSession
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.temporal.ChronoUnit

class WorkViewModel(private val repository: WorkRepository) : ViewModel() {
    private val _currentSession = MutableStateFlow<WorkSession?>(null)
    val currentSession: StateFlow<WorkSession?> = _currentSession.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _sessions = MutableStateFlow<List<WorkSession>>(emptyList())
    val sessions: StateFlow<List<WorkSession>> = _sessions.asStateFlow()

    init {
        loadCurrentSession()
        loadSessions()
    }

    private fun loadCurrentSession() {
        viewModelScope.launch {
            repository.getCurrentSession().collectLatest { session: WorkSession? ->
                _currentSession.value = session
                if (session != null) {
                    loadTasks(session.id)
                } else {
                    _tasks.value = emptyList()
                }
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
            val endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999)
            repository.getSessionsForDay(startOfDay, endOfDay).collectLatest { sessions: List<WorkSession> ->
                _sessions.value = sessions
            }
        }
    }

    private fun loadTasks(sessionId: Long) {
        viewModelScope.launch {
            repository.getTasksForSession(sessionId).collectLatest { tasks: List<Task> ->
                _tasks.value = tasks
            }
        }
    }

    fun startWorkSession(
        commuteTimeMinutes: Int,
        alarmTime: LocalDateTime,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ) {
        viewModelScope.launch {
            repository.startWorkSession(commuteTimeMinutes)
            loadCurrentSession()
        }
    }

    fun endWorkSession() {
        viewModelScope.launch {
            repository.endWorkSession()
            loadCurrentSession()
            loadSessions()
        }
    }

    fun addTask(description: String) {
        viewModelScope.launch {
            currentSession.value?.let { session ->
                repository.addTask(session.id, description)
                loadTasks(session.id)
            }
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            repository.completeTask(task)
            loadTasks(task.workSessionId)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            loadTasks(task.workSessionId)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            loadCurrentSession()
            loadSessions()
        }
    }

    val completedTasksCount: StateFlow<Int> = currentSession.flatMapLatest { session ->
        session?.let { repository.getCompletedTasksCount(it.id) } ?: flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val workProgress: StateFlow<Float> = combine(currentSession, tasks) { session, tasks ->
        session?.let { calculateProgress(it, tasks) } ?: 0f
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val timeRemaining: StateFlow<Duration> = currentSession.map { session ->
        session?.let { calculateTimeRemaining(it) } ?: Duration.ZERO
    }.stateIn(viewModelScope, SharingStarted.Lazily, Duration.ZERO)

    private fun calculateProgress(session: WorkSession, tasks: List<Task>): Float {
        if (tasks.isEmpty()) return 0f
        val completedCount = tasks.count { it.isCompleted }
        return completedCount.toFloat() / tasks.size
    }

    private fun calculateTimeRemaining(session: WorkSession): Duration {
        val now = LocalDateTime.now()
        val workDuration = Duration.ofHours(8)
        val endTime = session.entryTime.plus(workDuration)
        return Duration.between(now, endTime)
    }

    class Factory(private val repository: WorkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorkViewModel::class.java)) {
                return WorkViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 
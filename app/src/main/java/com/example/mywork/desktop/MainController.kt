package com.example.mywork.desktop

import com.example.mywork.data.Task
import com.example.mywork.data.WorkSession
import com.example.mywork.data.WorkRepository
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.collections.FXCollections
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration

class MainController {
    @FXML private lateinit var sessionStatusLabel: Label
    @FXML private lateinit var startSessionButton: Button
    @FXML private lateinit var endSessionButton: Button
    @FXML private lateinit var startTimeLabel: Label
    @FXML private lateinit var endTimeLabel: Label
    @FXML private lateinit var taskInputField: TextField
    @FXML private lateinit var tasksListView: ListView<Task>
    @FXML private lateinit var progressBar: ProgressBar
    @FXML private lateinit var progressLabel: Label
    @FXML private lateinit var timeRemainingLabel: Label

    private val repository = WorkRepository()
    private val tasks = FXCollections.observableArrayList<Task>()
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    @FXML
    fun initialize() {
        tasksListView.items = tasks
        tasksListView.cellFactory = { TaskListCell() }
        updateUI()
    }

    @FXML
    fun handleStartSession() {
        val now = LocalDateTime.now()
        val startTime = if (now.hour < 8) {
            now.withHour(8).withMinute(0).withSecond(0).withNano(0)
        } else {
            now
        }
        val endTime = startTime.plusHours(8)
        
        repository.startWorkSession(30, startTime, startTime, endTime)
        updateUI()
    }

    @FXML
    fun handleEndSession() {
        repository.endWorkSession()
        updateUI()
    }

    @FXML
    fun handleAddTask() {
        val description = taskInputField.text
        if (description.isNotEmpty()) {
            repository.addTask(description)
            taskInputField.clear()
            updateUI()
        }
    }

    private fun updateUI() {
        val currentSession = repository.getCurrentSession()
        
        if (currentSession != null) {
            sessionStatusLabel.text = "Active Session"
            startSessionButton.isDisable = true
            endSessionButton.isDisable = false
            
            startTimeLabel.text = currentSession.entryTime.format(formatter)
            endTimeLabel.text = currentSession.plannedExitTime.format(formatter)
            
            tasks.setAll(repository.getTasksForSession(currentSession.id))
            
            val progress = calculateProgress(currentSession)
            progressBar.progress = progress
            progressLabel.text = "${(progress * 100).toInt()}%"
            
            val timeRemaining = calculateTimeRemaining(currentSession)
            timeRemainingLabel.text = formatDuration(timeRemaining)
        } else {
            sessionStatusLabel.text = "No active session"
            startSessionButton.isDisable = false
            endSessionButton.isDisable = true
            startTimeLabel.text = "--:--"
            endTimeLabel.text = "--:--"
            tasks.clear()
            progressBar.progress = 0.0
            progressLabel.text = "0%"
            timeRemainingLabel.text = "--:--"
        }
    }

    private fun calculateProgress(session: WorkSession): Double {
        val tasks = repository.getTasksForSession(session.id)
        if (tasks.isEmpty()) return 0.0
        return tasks.count { it.isCompleted }.toDouble() / tasks.size
    }

    private fun calculateTimeRemaining(session: WorkSession): Duration {
        val now = LocalDateTime.now()
        return Duration.between(now, session.plannedExitTime)
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        return String.format("%02d:%02d", hours, minutes)
    }
}

class TaskListCell : ListCell<Task>() {
    override fun updateItem(task: Task?, empty: Boolean) {
        super.updateItem(task, empty)
        if (empty) {
            text = null
            graphic = null
        } else {
            val checkbox = CheckBox(task?.description ?: "")
            checkbox.isSelected = task?.isCompleted ?: false
            checkbox.setOnAction {
                task?.let { repository.completeTask(it) }
            }
            graphic = checkbox
        }
    }
} 
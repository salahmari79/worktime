package com.example.mywork.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.example.mywork.R
import com.example.mywork.data.WorkDatabase
import com.example.mywork.data.WorkRepository
import com.example.mywork.databinding.ActivityMainBinding
import com.example.mywork.service.WorkNotificationService
import com.example.mywork.service.WorkNotificationWorker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var notificationService: WorkNotificationService
    private var selectedStartTime: LocalTime = LocalTime.of(8, 0)
    private var selectedEndTime: LocalTime = LocalTime.of(16, 0) // 4:00 PM

    private val database = WorkDatabase.getDatabase(this)
    private val repository = WorkRepository(
        workSessionDao = database.workSessionDao(),
        taskDao = database.taskDao()
    )
    private val viewModel: WorkViewModel by viewModels {
        WorkViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        notificationService = WorkNotificationService(this)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Request notification permission for Android 13+
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskCheckedChanged = { task, isChecked ->
                viewModel.completeTask(task.copy(isCompleted = isChecked))
            },
            onTaskDelete = { task ->
                viewModel.deleteTask(task)
            }
        )

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }
    }

    private fun setupClickListeners() {
        binding.startSessionButton.setOnClickListener {
            val commuteTime = binding.commuteTimeInput.text.toString().toIntOrNull()
            if (commuteTime != null) {
                val now = LocalDateTime.now()
                val startTime = now.with(selectedStartTime)
                val endTime = now.with(selectedEndTime)
                
                // If the selected time is earlier today, use tomorrow's date
                val sessionStartTime = if (startTime.isBefore(now)) {
                    startTime.plusDays(1)
                } else {
                    startTime
                }
                
                val sessionEndTime = if (endTime.isBefore(now)) {
                    endTime.plusDays(1)
                } else {
                    endTime
                }
                
                val alarmTime = sessionStartTime.minusMinutes(commuteTime.toLong())
                viewModel.startWorkSession(commuteTime, alarmTime, sessionStartTime, sessionEndTime)
                notificationService.scheduleMorningAlarm(alarmTime)
                
                // Schedule hourly notifications
                scheduleHourlyNotifications()
            } else {
                Toast.makeText(this, "Please enter valid commute time", Toast.LENGTH_SHORT).show()
            }
        }

        binding.selectStartTimeButton.setOnClickListener {
            showTimePickerDialog(true)
        }

        binding.selectEndTimeButton.setOnClickListener {
            showTimePickerDialog(false)
        }

        binding.endSessionButton.setOnClickListener {
            viewModel.endWorkSession()
            
            // Cancel hourly notifications
            cancelHourlyNotifications()
        }

        binding.addTaskButton.setOnClickListener {
            val taskDescription = binding.taskInput.text.toString()
            if (taskDescription.isNotBlank()) {
                viewModel.addTask(taskDescription)
                binding.taskInput.text?.clear()
            }
        }

        binding.clearDataButton.setOnClickListener {
            viewModel.clearAllData()
            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimePickerDialog(isStartTime: Boolean) {
        val currentTime = if (isStartTime) selectedStartTime else selectedEndTime
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val newTime = LocalTime.of(hourOfDay, minute)
                if (isStartTime) {
                    selectedStartTime = newTime
                    binding.selectedStartTimeText.text = "Start time: ${selectedStartTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
                } else {
                    selectedEndTime = newTime
                    binding.selectedEndTimeText.text = "End time: ${selectedEndTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
                }
            },
            currentTime.hour,
            currentTime.minute,
            false
        )
        timePickerDialog.show()
    }

    private fun scheduleHourlyNotifications() {
        // Cancel any existing work
        cancelHourlyNotifications()
        
        // Create a periodic work request that runs every hour
        val hourlyWorkRequest = PeriodicWorkRequestBuilder<WorkNotificationWorker>(
            1, TimeUnit.HOURS
        ).build()
        
        // Enqueue the work
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            HOURLY_NOTIFICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            hourlyWorkRequest
        )
    }
    
    private fun cancelHourlyNotifications() {
        WorkManager.getInstance(this).cancelUniqueWork(HOURLY_NOTIFICATION_WORK_NAME)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.tasks.collectLatest { tasks ->
                taskAdapter.submitList(tasks)
            }
        }

        lifecycleScope.launch {
            viewModel.workProgress.collectLatest { progress ->
                binding.workProgressIndicator.progress = (progress * 100).toInt()
            }
        }

        lifecycleScope.launch {
            viewModel.timeRemaining.collectLatest { duration ->
                updateTimeRemaining(duration)
            }
        }
    }

    private fun updateTimeRemaining(duration: Duration) {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        binding.timeRemainingText.text = getString(
            R.string.time_remaining_format,
            hours,
            minutes
        )
    }
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val HOURLY_NOTIFICATION_WORK_NAME = "hourly_notification_work"
    }
} 
package com.example.mywork

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mywork.data.WorkDatabase
import com.example.mywork.data.WorkRepository
import com.example.mywork.data.WorkSession
import com.example.mywork.data.Task
import com.example.mywork.ui.TaskAdapter
import com.example.mywork.ui.WorkViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.mywork.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: WorkViewModel
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, WorkViewModel.Factory((application as WorkApplication).repository))
            .get(WorkViewModel::class.java)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskComplete = { task -> viewModel.completeTask(task) },
            onTaskDelete = { task -> viewModel.deleteTask(task) }
        )
        binding.tasksRecyclerView.adapter = taskAdapter
    }

    private fun setupClickListeners() {
        binding.startSessionButton.setOnClickListener {
            val commuteTime = 30 // Default commute time in minutes
            val now = LocalDateTime.now()
            val startTime = if (now.hour < 8) {
                now.withHour(8).withMinute(0).withSecond(0).withNano(0)
            } else {
                now
            }
            val endTime = startTime.plusHours(8)
            
            viewModel.startWorkSession(commuteTime, startTime, startTime, endTime)
        }

        binding.endSessionButton.setOnClickListener {
            viewModel.endWorkSession()
        }

        binding.addTaskButton.setOnClickListener {
            val taskDescription = binding.taskInputEditText.text.toString()
            if (taskDescription.isNotEmpty()) {
                viewModel.addTask(taskDescription)
                binding.taskInputEditText.text.clear()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.currentSession.observe(this) { session ->
            updateSessionUI(session)
        }

        viewModel.tasks.observe(this) { tasks ->
            taskAdapter.submitList(tasks)
        }

        viewModel.sessions.observe(this) { sessions ->
            // Update sessions list if needed
        }
    }

    private fun updateSessionUI(session: WorkSession?) {
        if (session != null) {
            binding.sessionStatusText.text = "Active Session"
            binding.startSessionButton.isEnabled = false
            binding.endSessionButton.isEnabled = true
            
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            binding.selectedStartTimeText.text = "Start: ${session.entryTime.format(formatter)}"
            binding.selectedEndTimeText.text = "End: ${session.plannedExitTime.format(formatter)}"
        } else {
            binding.sessionStatusText.text = "No active session"
            binding.startSessionButton.isEnabled = true
            binding.endSessionButton.isEnabled = false
            binding.selectedStartTimeText.text = ""
            binding.selectedEndTimeText.text = ""
        }
    }
} 
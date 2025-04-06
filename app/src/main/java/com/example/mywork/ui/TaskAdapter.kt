package com.example.mywork.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mywork.R
import com.example.mywork.data.Task

class TaskAdapter(
    private val onTaskCheckedChanged: (Task, Boolean) -> Unit,
    private val onTaskDelete: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view, onTaskCheckedChanged, onTaskDelete)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(
        itemView: View,
        private val onTaskCheckedChanged: (Task, Boolean) -> Unit,
        private val onTaskDelete: (Task) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val taskText: TextView = itemView.findViewById(R.id.taskText)

        fun bind(task: Task) {
            taskText.text = task.description
            taskText.setCompoundDrawablesWithIntrinsicBounds(
                if (task.isCompleted) R.drawable.ic_check else R.drawable.ic_circle,
                0, 0, 0
            )
            itemView.setOnClickListener { 
                onTaskCheckedChanged(task, !task.isCompleted)
            }
            itemView.setOnLongClickListener {
                onTaskDelete(task)
                true
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
} 
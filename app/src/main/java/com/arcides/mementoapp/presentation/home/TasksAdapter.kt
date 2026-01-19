package com.arcides.mementoapp.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcides.mementoapp.databinding.ItemTaskBinding
import com.arcides.mementoapp.domain.models.Task

class TasksAdapter(
    private val onTaskChecked: (String, Boolean) -> Unit,
    private val onTaskDeleted: (String) -> Unit
) : ListAdapter<Task, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.taskTitle.text = task.title
            binding.taskDescription.text = task.description

            // Prioridad
            val priorityText = when (task.priority) {
                Task.Priority.HIGH -> "ALTA"
                Task.Priority.MEDIUM -> "MEDIA"
                Task.Priority.LOW -> "BAJA"
            }
            binding.taskPriority.text = priorityText

            // Estado (checkbox)
            binding.taskCheckbox.isChecked = task.status == Task.TaskStatus.COMPLETED

            // Listeners
            binding.taskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task.id, isChecked)
            }

            binding.deleteButton.setOnClickListener {
                onTaskDeleted(task.id)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
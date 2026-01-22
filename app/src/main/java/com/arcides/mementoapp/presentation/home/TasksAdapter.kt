package com.arcides.mementoapp.presentation.home

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcides.mementoapp.databinding.ItemTaskBinding
import com.arcides.mementoapp.domain.models.Task

class TasksAdapter(
    private val onTaskChecked: (String, Boolean) -> Unit,
    private val onTaskEdit: (Task) -> Unit,
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
            val isCompleted = task.status == Task.TaskStatus.COMPLETED
            
            binding.taskTitle.text = task.title
            binding.taskDescription.text = task.description
            
            // Estilo según estado (completado o no)
            if (isCompleted) {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.6f
            } else {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }

            // Prioridad
            val priorityText = when (task.priority) {
                Task.Priority.HIGH -> "ALTA"
                Task.Priority.MEDIUM -> "MEDIA"
                Task.Priority.LOW -> "BAJA"
            }
            binding.taskPriority.text = priorityText
            
            // Categoría Badge (Mejora: Color y Nombre)
            if (task.category != null) {
                binding.categoryBadge.visibility = View.VISIBLE
                binding.categoryBadge.text = task.category.name.uppercase()
                try {
                    binding.categoryBadge.background.setTint(Color.parseColor(task.category.color))
                } catch (e: Exception) {
                    binding.categoryBadge.background.setTint(Color.GRAY)
                }
            } else {
                binding.categoryBadge.visibility = View.GONE
            }

            // Estado (checkbox)
            binding.taskCheckbox.isChecked = isCompleted

            // Listeners
            binding.taskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task.id, isChecked)
            }

            binding.editButton.setOnClickListener {
                onTaskEdit(task)
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
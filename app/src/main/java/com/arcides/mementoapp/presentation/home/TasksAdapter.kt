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
            
            // Estilo visual según estado
            updateTaskStyle(isCompleted)

            // Prioridad
            binding.taskPriority.text = when (task.priority) {
                Task.Priority.HIGH -> "ALTA"
                Task.Priority.MEDIUM -> "MEDIA"
                Task.Priority.LOW -> "BAJA"
            }
            
            // Categoría Badge
            val category = task.category
            if (category != null) {
                binding.categoryBadge.visibility = View.VISIBLE
                binding.categoryBadge.text = category.name.uppercase()
                try {
                    binding.categoryBadge.background.setTint(Color.parseColor(category.color))
                } catch (e: Exception) {
                    binding.categoryBadge.background.setTint(Color.GRAY)
                }
            } else {
                binding.categoryBadge.visibility = View.GONE
            }

            // Estado (checkbox) - Evitar disparar el listener al hacer el binding
            binding.taskCheckbox.setOnCheckedChangeListener(null)
            binding.taskCheckbox.isChecked = isCompleted
            binding.taskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task.id, isChecked)
            }

            // Listeners de botones
            binding.editButton.setOnClickListener { onTaskEdit(task) }
            binding.deleteButton.setOnClickListener { onTaskDeleted(task.id) }
        }

        private fun updateTaskStyle(isCompleted: Boolean) {
            if (isCompleted) {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.6f
            } else {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            // Se debe comparar manualmente 'category' porque está marcada con @Ignore 
            // y no forma parte del equals() automático de la data class Task.
            return oldItem == newItem && oldItem.category == newItem.category
        }
    }
}

package com.arcides.mementoapp.presentation.home

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.ItemTaskBinding
import com.arcides.mementoapp.domain.models.Task
import java.text.SimpleDateFormat
import java.util.Locale

class TasksAdapter(
    private val onStatusChange: (String, Task.TaskStatus) -> Unit,
    private val onTaskEdit: (Task) -> Unit,
    private val onTaskDeleted: (String) -> Unit
) : ListAdapter<Task, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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
            
            // Fecha y Hora
            if (task.dueDate != null) {
                binding.dueDateContainer.visibility = View.VISIBLE
                binding.taskDueDate.text = dateTimeFormatter.format(task.dueDate)
            } else {
                binding.dueDateContainer.visibility = View.GONE
            }

            // Actualizar estilo visual y botones según estado
            updateStatusUI(task)

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

            // Click en el botón de estado para abrir menú de cambio de estado
            binding.statusButton.setOnClickListener { view ->
                showStatusPopupMenu(view, task)
            }

            // Listeners de botones
            binding.editButton.setOnClickListener { onTaskEdit(task) }
            binding.deleteButton.setOnClickListener { onTaskDeleted(task.id) }
        }

        private fun updateStatusUI(task: Task) {
            when (task.status) {
                Task.TaskStatus.PENDING -> {
                    binding.statusButton.setImageResource(android.R.drawable.ic_menu_more)
                    binding.statusButton.colorFilter = null
                    binding.statusLabel.text = "Pendiente"
                    binding.taskTitle.paintFlags = binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    binding.root.alpha = 1.0f
                }
                Task.TaskStatus.IN_PROGRESS -> {
                    binding.statusButton.setImageResource(android.R.drawable.ic_menu_more)
                    binding.statusButton.setColorFilter(Color.parseColor("#FFC107")) // Ámbar/Amarillo
                    binding.statusLabel.text = "En Progreso"
                    binding.taskTitle.paintFlags = binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    binding.root.alpha = 1.0f
                }
                Task.TaskStatus.COMPLETED -> {
                    binding.statusButton.setImageResource(android.R.drawable.ic_menu_more)
                    val greenColor = try {
                        Color.parseColor("#4CAF50")
                    } catch (e: Exception) {
                        Color.GREEN
                    }
                    binding.statusButton.setColorFilter(greenColor)
                    binding.statusLabel.text = "Completada"
                    binding.taskTitle.paintFlags = binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    binding.root.alpha = 0.6f
                }
            }
        }

        private fun showStatusPopupMenu(view: View, task: Task) {
            val popup = PopupMenu(view.context, view)
            popup.menu.add(0, 1, 0, "Pendiente")
            popup.menu.add(0, 2, 1, "En Progreso")
            popup.menu.add(0, 3, 2, "Completada")
            
            popup.setOnMenuItemClickListener { item ->
                val newStatus = when (item.itemId) {
                    1 -> Task.TaskStatus.PENDING
                    2 -> Task.TaskStatus.IN_PROGRESS
                    3 -> Task.TaskStatus.COMPLETED
                    else -> task.status
                }
                if (newStatus != task.status) {
                    onStatusChange(task.id, newStatus)
                }
                true
            }
            popup.show()
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem && oldItem.category == newItem.category
        }
    }
}

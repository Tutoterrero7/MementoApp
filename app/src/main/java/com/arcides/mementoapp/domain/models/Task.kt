package com.arcides.mementoapp.domain.models

import java.util.Date

data class Task(
    val id: String = "", // Firebase generará IDs únicos
    val title: String = "",
    val description: String = "",
    val dueDate: Date? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val category: Category? = null,
    val userId: String = "", // Para filtrar por usuario
    val createdAt: Date = Date()
) {
    enum class Priority { LOW, MEDIUM, HIGH }
    enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }
}
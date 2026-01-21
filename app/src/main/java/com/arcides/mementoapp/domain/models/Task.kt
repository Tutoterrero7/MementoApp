package com.arcides.mementoapp.domain.models

import java.util.Date

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: Date? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val categoryId: String = "",
    val userId: String = "",
    val createdAt: Date = Date()
) {
    enum class Priority { LOW, MEDIUM, HIGH }
    enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }
}
package com.arcides.mementoapp.domain.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val dueDate: Date? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val categoryId: String = "",
    val userId: String = "",
    val createdAt: Date = Date()
) {
    // Room will use the primary constructor above.
    // We move the category field here so it's not part of the primary constructor
    // but can still be used for UI purposes.
    @Ignore
    var category: Category? = null

    enum class Priority { LOW, MEDIUM, HIGH }
    enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }
}

package com.arcides.mementoapp.domain.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.Date
import java.util.UUID

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    @Serializable(with = DateSerializer::class)
    val dueDate: Date? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val categoryId: String = "",
    val userId: String = "",
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date()
) {
    @Ignore
    @Transient
    var category: Category? = null

    enum class Priority { LOW, MEDIUM, HIGH }
    enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }
}

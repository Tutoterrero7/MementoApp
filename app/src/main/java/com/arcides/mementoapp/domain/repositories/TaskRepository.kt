package com.arcides.mementoapp.domain.repositories

import com.arcides.mementoapp.domain.models.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun createTask(task: Task): String
    suspend fun updateTask(task: Task)
    suspend fun toggleTaskStatus(taskId: String, newStatus: Task.TaskStatus)
    suspend fun deleteTask(taskId: String)
}

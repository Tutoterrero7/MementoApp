package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.data.local.TaskDao
import com.arcides.mementoapp.data.local.CategoryDao
import com.arcides.mementoapp.domain.models.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao
) {
    
    // 1. Obtener tareas en tiempo real
    fun getTasksStream(): Flow<List<Task>> = taskDao.getTasksStream()
    
    // 2. Crear nueva tarea
    suspend fun createTask(task: Task): String {
        taskDao.insertTask(task)
        
        // Incrementar contador en categoría si existe
        if (task.categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(task.categoryId, 1)
        }
        
        return task.id
    }

    // 2.5 Actualizar tarea existente
    suspend fun updateTask(task: Task) {
        // Obtener la tarea antigua para ver si cambió la categoría
        val oldCategoryId = taskDao.getCategoryIdForTask(task.id) ?: ""

        taskDao.updateTask(task)

        // Si la categoría cambió, actualizar contadores
        if (oldCategoryId != task.categoryId) {
            if (oldCategoryId.isNotBlank()) {
                categoryDao.updateTaskCount(oldCategoryId, -1)
            }
            if (task.categoryId.isNotBlank()) {
                categoryDao.updateTaskCount(task.categoryId, 1)
            }
        }
    }
    
    // 3. Cambiar estado de tarea
    suspend fun toggleTaskStatus(taskId: String, newStatus: Task.TaskStatus) {
        val task = taskDao.getTaskById(taskId)
        task?.let {
            taskDao.updateTask(it.copy(status = newStatus))
        }
    }
    
    // 4. Eliminar tarea
    suspend fun deleteTask(taskId: String) {
        val categoryId = taskDao.getCategoryIdForTask(taskId) ?: ""
        
        // Eliminar la tarea
        taskDao.deleteById(taskId)
        
        // Decrementar contador en categoría si existe
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, -1)
        }
    }
}
package com.arcides.mementoapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.data.repositories.TaskRepository
import com.arcides.mementoapp.domain.models.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    // Estado de las tareas
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Mensajes de error/éxito
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        // Cargar tareas al iniciar
        loadTasks()
    }

    fun loadTasks() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // TODO: Reemplazar con llamada real al repository
                // Por ahora, datos de prueba
                val sampleTasks = listOf(
                    Task(
                        id = "1",
                        title = "Tarea de ejemplo 1",
                        description = "Esta es una tarea de prueba",
                        priority = Task.Priority.HIGH
                    ),
                    Task(
                        id = "2",
                        title = "Tarea de ejemplo 2",
                        description = "Otra tarea de prueba",
                        priority = Task.Priority.MEDIUM
                    )
                )
                _tasks.value = sampleTasks
                _message.value = "Tareas cargadas"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Crear nueva tarea
    fun createTask(title: String, description: String = "") {
        viewModelScope.launch {
            try {
                val newTask = Task(
                    id = System.currentTimeMillis().toString(), // ID temporal
                    title = title,
                    description = description,
                    status = Task.TaskStatus.PENDING
                )
                _tasks.value = _tasks.value + newTask
                _message.value = "Tarea creada: $title"
            } catch (e: Exception) {
                _message.value = "Error al crear tarea: ${e.message}"
            }
        }
    }

    // Cambiar estado de tarea
    fun toggleTaskStatus(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                val updatedTasks = _tasks.value.map { task ->
                    if (task.id == taskId) {
                        task.copy(
                            status = if (isCompleted) {
                                Task.TaskStatus.COMPLETED
                            } else {
                                Task.TaskStatus.PENDING
                            }
                        )
                    } else {
                        task
                    }
                }
                _tasks.value = updatedTasks
                _message.value = if (isCompleted) "Tarea completada" else "Tarea pendiente"
            } catch (e: Exception) {
                _message.value = "Error al cambiar estado: ${e.message}"
            }
        }
    }

    // Eliminar tarea
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                val filteredTasks = _tasks.value.filter { it.id != taskId }
                _tasks.value = filteredTasks
                _message.value = "Tarea eliminada"
            } catch (e: Exception) {
                _message.value = "Error al eliminar: ${e.message}"
            }
        }
    }

    // Limpiar mensajes
    fun clearMessage() {
        _message.value = null
    }
}
package com.arcides.mementoapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.data.repositories.CategoryRepository
import com.arcides.mementoapp.data.repositories.TaskRepository
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
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

    // Añade este StateFlow para categorías
    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesStream()
        .map { categories -> categories }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    // Crear nueva tarea modificada para incluir categoría
    fun createTask(
        title: String,
        description: String = "",
        priority: Task.Priority = Task.Priority.MEDIUM,
        category: Category? = null
    ) {
        viewModelScope.launch {
            try {
                val newTask = Task(
                    title = title,
                    description = description,
                    priority = priority,
                    category = category,
                    status = Task.TaskStatus.PENDING
                )

                val taskId = taskRepository.createTask(newTask)

                // Incrementar contador de categoría si existe
                category?.let {
                    categoryRepository.incrementTaskCount(it.id)
                }

                _message.value = "Tarea creada: $title"
                // Nota: loadTasks() debería ser llamado o las tareas deberían observarse desde el repo
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
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
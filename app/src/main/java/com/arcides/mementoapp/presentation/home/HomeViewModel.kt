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

    // Estado de las tareas (Flow desde Firestore)
    val tasks: StateFlow<List<Task>> = taskRepository.getTasksStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Cambiado a Eagerly para que cargue de inmediato
            initialValue = emptyList()
        )

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Mensajes de error/éxito
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // StateFlow para categorías
    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Importante: Eagerly activa el flujo de inmediato
            initialValue = emptyList()
        )

    init {
        // Asegurar que existan categorías por defecto al iniciar la app
        createDefaultCategories()
    }

    private fun createDefaultCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.createDefaultCategoriesIfNeeded()
            } catch (e: Exception) {
                // Silencioso, si falla no bloqueamos la app
            }
        }
    }

    // Crear nueva tarea
    fun createTask(
        title: String,
        description: String = "",
        priority: Task.Priority = Task.Priority.MEDIUM,
        categoryId: String = ""
    ) {
        if (title.isBlank()) {
            _message.value = "El título no puede estar vacío"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newTask = Task(
                    title = title,
                    description = description,
                    priority = priority,
                    categoryId = categoryId,
                    status = Task.TaskStatus.PENDING
                )

                taskRepository.createTask(newTask)
                _message.value = "Tarea creada: $title"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Actualizar tarea existente
    fun updateTask(
        id: String,
        title: String,
        description: String,
        priority: Task.Priority,
        categoryId: String,
        status: Task.TaskStatus
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedTask = Task(
                    id = id,
                    title = title,
                    description = description,
                    priority = priority,
                    categoryId = categoryId,
                    status = status
                )
                taskRepository.updateTask(updatedTask)
                _message.value = "Tarea actualizada"
            } catch (e: Exception) {
                _message.value = "Error al actualizar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Cambiar estado de tarea
    fun toggleTaskStatus(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                val newStatus = if (isCompleted) {
                    Task.TaskStatus.COMPLETED
                } else {
                    Task.TaskStatus.PENDING
                }
                taskRepository.toggleTaskStatus(taskId, newStatus)
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    // Eliminar tarea
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                taskRepository.deleteTask(taskId)
                _message.value = "Tarea eliminada"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
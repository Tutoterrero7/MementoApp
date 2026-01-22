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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    enum class TaskFilter { ALL, PENDING, COMPLETED }

    sealed class HomeUiState {
        object Loading : HomeUiState()
        data class Success(val tasks: List<Task>) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }

    // Filtro actual
    private val _currentFilter = MutableStateFlow(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter

    // Flujo de categorías
    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Estado centralizado con filtrado dinámico
    val uiState: StateFlow<HomeUiState> = combine(
        taskRepository.getTasksStream(),
        categories,
        _currentFilter
    ) { tasks, categories, filter ->
        val enrichedTasks = tasks.map { task ->
            task.copy(category = categories.find { it.id == task.categoryId })
        }
        
        val filteredTasks = when (filter) {
            TaskFilter.ALL -> enrichedTasks
            TaskFilter.PENDING -> enrichedTasks.filter { it.status != Task.TaskStatus.COMPLETED }
            TaskFilter.COMPLETED -> enrichedTasks.filter { it.status == Task.TaskStatus.COMPLETED }
        }
        
        HomeUiState.Success(filteredTasks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        createDefaultCategories()
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    private fun createDefaultCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.createDefaultCategoriesIfNeeded()
            } catch (e: Exception) { /* Silencioso */ }
        }
    }

    fun createTask(title: String, description: String = "", priority: Task.Priority = Task.Priority.MEDIUM, categoryId: String = "") {
        if (title.isBlank()) {
            _message.value = "El título es requerido"
            return
        }
        viewModelScope.launch {
            try {
                val newTask = Task(title = title, description = description, priority = priority, categoryId = categoryId)
                taskRepository.createTask(newTask)
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    fun updateTask(id: String, title: String, description: String, priority: Task.Priority, categoryId: String, status: Task.TaskStatus) {
        viewModelScope.launch {
            try {
                val updatedTask = Task(id = id, title = title, description = description, priority = priority, categoryId = categoryId, status = status)
                taskRepository.updateTask(updatedTask)
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    fun toggleTaskStatus(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                val newStatus = if (isCompleted) Task.TaskStatus.COMPLETED else Task.TaskStatus.PENDING
                taskRepository.toggleTaskStatus(taskId, newStatus)
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
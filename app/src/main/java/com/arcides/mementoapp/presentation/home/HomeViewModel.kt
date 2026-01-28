package com.arcides.mementoapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.data.repositories.CategoryRepository
import com.arcides.mementoapp.data.repositories.TaskRepository
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Estados para filtros (RF11, RF13)
    private val _searchQuery = MutableStateFlow<String?>(null)
    private val _statusFilter = MutableStateFlow<Task.TaskStatus?>(null)
    private val _priorityFilter = MutableStateFlow<Task.Priority?>(null)
    private val _categoryFilter = MutableStateFlow<String?>(null)

    // Flujo de categorías para la UI
    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // UI State reactivo que combina filtros y búsqueda (RF10, RF11, RF12, RF13)
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery, _statusFilter, _priorityFilter, _categoryFilter
    ) { query, status, priority, categoryId ->
        Filters(query, status, priority, categoryId)
    }.flatMapLatest { filters ->
        taskRepository.getFilteredTasksStream(
            query = filters.query,
            status = filters.status,
            priority = filters.priority,
            categoryId = filters.categoryId
        ).combine(categories) { tasks, categories ->
            val enrichedTasks = tasks.map { task ->
                task.copy().apply {
                    category = categories.find { it.id == task.categoryId }
                }
            }
            HomeUiState.Success(enrichedTasks)
        }
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

    // Funciones para actualizar filtros (RF11, RF13)
    fun setSearchQuery(query: String?) {
        _searchQuery.value = if (query.isNullOrBlank()) null else query
    }

    fun setStatusFilter(status: Task.TaskStatus?) {
        _statusFilter.value = status
    }

    fun setPriorityFilter(priority: Task.Priority?) {
        _priorityFilter.value = priority
    }

    fun setCategoryFilter(categoryId: String?) {
        _categoryFilter.value = categoryId
    }

    private fun createDefaultCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.createDefaultCategoriesIfNeeded()
            } catch (e: Exception) { /* Log error */ }
        }
    }

    // Operaciones CRUD (RF6, RF7, RF8, RF9)
    fun createTask(title: String, description: String, priority: Task.Priority, categoryId: String) {
        viewModelScope.launch {
            taskRepository.createTask(Task(title = title, description = description, priority = priority, categoryId = categoryId))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun toggleTaskStatus(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val newStatus = if (isCompleted) Task.TaskStatus.COMPLETED else Task.TaskStatus.PENDING
            taskRepository.toggleTaskStatus(taskId, newStatus)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun clearMessage() { _message.value = null }

    data class Filters(
        val query: String?,
        val status: Task.TaskStatus?,
        val priority: Task.Priority?,
        val categoryId: String?
    )

    sealed class HomeUiState {
        object Loading : HomeUiState()
        data class Success(val tasks: List<Task>) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }
}

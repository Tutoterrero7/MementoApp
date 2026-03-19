package com.arcides.mementoapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.domain.repositories.CategoryRepository
import com.arcides.mementoapp.domain.repositories.TaskRepository
import com.arcides.mementoapp.domain.repositories.AuthRepository
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow<String?>(null)
    private val _statusFilter = MutableStateFlow<Task.TaskStatus?>(null)
    private val _priorityFilter = MutableStateFlow<Task.Priority?>(null)
    private val _categoryFilter = MutableStateFlow<String?>(null)

    // Estado de carga inicial para la sincronización
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<Category>> = authRepository.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            categoryRepository.getCategoriesStream(user.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.currentUser.filterNotNull(),
        _searchQuery, _statusFilter, _priorityFilter, _categoryFilter
    ) { user, query, status, priority, categoryId ->
        user.id to Filters(query, status, priority, categoryId)
    }.flatMapLatest { (userId, filters) ->
        taskRepository.getFilteredTasksStream(
            userId = userId,
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
        refreshData()
    }

    // Sincronizar todo desde Supabase
    fun refreshData() {
        viewModelScope.launch {
            val userId = authRepository.currentUser.first()?.id ?: return@launch
            
            _isSyncing.value = true
            try {
                // 1. Asegurar categorías por defecto localmente
                categoryRepository.createDefaultCategoriesIfNeeded(userId)
                
                // 2. Traer categorías de la nube
                categoryRepository.fetchCategoriesFromRemote(userId)
                
                // 3. Traer tareas de la nube
                taskRepository.fetchTasksFromRemote(userId)
                
            } catch (e: Exception) {
                _message.value = "Error al sincronizar: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

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

    fun createTask(title: String, description: String, priority: Task.Priority, categoryId: String, dueDate: Date? = null) {
        viewModelScope.launch {
            val userId = authRepository.currentUser.first()?.id ?: return@launch
            taskRepository.createTask(Task(
                title = title,
                description = description,
                priority = priority,
                categoryId = categoryId,
                dueDate = dueDate,
                userId = userId
            ))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: Task.TaskStatus) {
        viewModelScope.launch {
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

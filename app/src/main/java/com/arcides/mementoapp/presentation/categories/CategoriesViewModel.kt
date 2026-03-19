package com.arcides.mementoapp.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.domain.repositories.AuthRepository
import com.arcides.mementoapp.domain.repositories.CategoryRepository
import com.arcides.mementoapp.domain.models.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Lista de categorías vinculada al usuario actual
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

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Mensajes
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Crear categoría
    fun createCategory(name: String, color: String) {
        if (name.isBlank()) {
            _message.value = "El nombre no puede estar vacío"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.currentUser.first()?.id
                if (userId != null) {
                    val newCategory = Category(
                        name = name.trim(),
                        color = color,
                        userId = userId
                    )
                    categoryRepository.createCategory(newCategory)
                    _message.value = "Categoría creada: $name"
                } else {
                    _message.value = "Error: Usuario no autenticado"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Actualizar categoría
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category)
                _message.value = "Categoría actualizada"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    // Eliminar categoría
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.currentUser.first()?.id
                if (userId != null) {
                    categoryRepository.deleteCategory(categoryId, userId)
                    _message.value = "Categoría eliminada"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Crear categorías por defecto
    fun createDefaultCategories() {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser.first()?.id
                if (userId != null) {
                    categoryRepository.createDefaultCategoriesIfNeeded(userId)
                    _message.value = "Categorías por defecto creadas"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    // Limpiar mensajes
    fun clearMessage() {
        _message.value = null
    }
}

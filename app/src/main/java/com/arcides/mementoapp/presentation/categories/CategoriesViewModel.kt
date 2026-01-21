package com.arcides.mementoapp.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.data.repositories.CategoryRepository
import com.arcides.mementoapp.domain.models.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Lista de categorías
    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesStream()
        .map { categories -> categories }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Mensajes
    private val _message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val message = _message as kotlinx.coroutines.flow.StateFlow<String?>

    // Crear categoría
    fun createCategory(name: String, color: String) {
        if (name.isBlank()) {
            _message.value = "El nombre no puede estar vacío"
            return
        }

        viewModelScope.launch {
            try {
                val newCategory = Category(
                    name = name.trim(),
                    color = color
                )
                categoryRepository.createCategory(newCategory)
                _message.value = "Categoría creada: $name"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
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
            try {
                categoryRepository.deleteCategory(categoryId)
                _message.value = "Categoría eliminada"
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
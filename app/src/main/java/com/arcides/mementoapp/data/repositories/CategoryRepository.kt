package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.data.local.CategoryDao
import com.arcides.mementoapp.domain.models.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    // 1. Obtener categorías en tiempo real
    fun getCategoriesStream(): Flow<List<Category>> = categoryDao.getCategoriesStream()

    // 2. Crear categoría
    suspend fun createCategory(category: Category): String {
        categoryDao.insertCategory(category)
        return category.id
    }

    // 3. Actualizar categoría
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    // 4. Eliminar categoría
    suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteById(categoryId)
    }

    // 5. Incrementar contador de tareas en categoría
    suspend fun incrementTaskCount(categoryId: String) {
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, 1)
        }
    }

    // 6. Decrementar contador de tareas en categoría
    suspend fun decrementTaskCount(categoryId: String) {
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, -1)
        }
    }

    // 7. Verificar si el usuario tiene categorías
    suspend fun hasCategories(): Boolean {
        return categoryDao.getCount() > 0
    }

    // 8. Crear categorías por defecto
    suspend fun createDefaultCategoriesIfNeeded() {
        if (!hasCategories()) {
            val defaultCats = Category.defaultCategories()
            for (category in defaultCats) {
                createCategory(category)
            }
        }
    }
}
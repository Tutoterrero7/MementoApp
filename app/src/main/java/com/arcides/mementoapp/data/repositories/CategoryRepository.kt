package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.data.local.CategoryDao
import com.arcides.mementoapp.domain.models.Category
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val supabaseClient: SupabaseClient
) {
    // 1. Obtener categorías en tiempo real (Local)
    fun getCategoriesStream(): Flow<List<Category>> = categoryDao.getCategoriesStream()

    // Sincronizar desde Supabase
    suspend fun fetchCategoriesFromRemote() {
        try {
            val remoteCategories = supabaseClient.postgrest["categories"]
                .select()
                .decodeList<Category>()

            for (category in remoteCategories) {
                categoryDao.insertCategory(category)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. Crear categoría
    suspend fun createCategory(category: Category): String {
        categoryDao.insertCategory(category)
        try {
            supabaseClient.postgrest["categories"].insert(category)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return category.id
    }

    // 3. Actualizar categoría
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
        try {
            supabaseClient.postgrest["categories"].update(category) {
                filter {
                    eq("id", category.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 4. Eliminar categoría
    suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteById(categoryId)
        try {
            supabaseClient.postgrest["categories"].delete {
                filter {
                    eq("id", categoryId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

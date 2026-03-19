package com.arcides.mementoapp.domain.repositories

import com.arcides.mementoapp.domain.models.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesStream(userId: String): Flow<List<Category>>
    suspend fun fetchCategoriesFromRemote(userId: String)
    suspend fun createCategory(category: Category): String
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(categoryId: String, userId: String)
    suspend fun incrementTaskCount(categoryId: String, userId: String)
    suspend fun decrementTaskCount(categoryId: String, userId: String)
    suspend fun hasCategories(userId: String): Boolean
    suspend fun createDefaultCategoriesIfNeeded(userId: String)
    suspend fun updateTaskCount(categoryId: String, userId: String, delta: Int)
}

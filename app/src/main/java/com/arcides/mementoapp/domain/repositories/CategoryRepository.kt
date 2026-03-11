package com.arcides.mementoapp.domain.repositories

import com.arcides.mementoapp.domain.models.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesStream(): Flow<List<Category>>
    suspend fun fetchCategoriesFromRemote()
    suspend fun createCategory(category: Category): String
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(categoryId: String)
    suspend fun incrementTaskCount(categoryId: String)
    suspend fun decrementTaskCount(categoryId: String)
    suspend fun hasCategories(): Boolean
    suspend fun createDefaultCategoriesIfNeeded()
    suspend fun updateTaskCount(categoryId: String, delta: Int)
}

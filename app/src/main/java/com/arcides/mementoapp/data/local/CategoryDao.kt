package com.arcides.mementoapp.data.local

import androidx.room.*
import com.arcides.mementoapp.domain.models.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getCategoriesStream(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE categories SET taskCount = taskCount + :increment WHERE id = :categoryId")
    suspend fun updateTaskCount(categoryId: String, increment: Int)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
}
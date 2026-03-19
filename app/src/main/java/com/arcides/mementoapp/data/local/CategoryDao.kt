package com.arcides.mementoapp.data.local

import androidx.room.*
import com.arcides.mementoapp.domain.models.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun getCategoriesStream(userId: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("UPDATE categories SET taskCount = taskCount + :increment WHERE id = :categoryId AND userId = :userId")
    suspend fun updateTaskCount(categoryId: String, userId: String, increment: Int)

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId")
    suspend fun getCount(userId: String): Int
}

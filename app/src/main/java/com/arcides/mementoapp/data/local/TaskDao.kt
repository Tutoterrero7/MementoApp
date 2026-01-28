package com.arcides.mementoapp.data.local

import androidx.room.*
import com.arcides.mementoapp.domain.models.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getTasksStream(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE (:query IS NULL OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        AND (:status IS NULL OR status = :status)
        AND (:priority IS NULL OR priority = :priority)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY createdAt DESC
    """)
    fun getFilteredTasksStream(
        query: String? = null,
        status: Task.TaskStatus? = null,
        priority: Task.Priority? = null,
        categoryId: String? = null
    ): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT categoryId FROM tasks WHERE id = :taskId")
    suspend fun getCategoryIdForTask(taskId: String): String?
}
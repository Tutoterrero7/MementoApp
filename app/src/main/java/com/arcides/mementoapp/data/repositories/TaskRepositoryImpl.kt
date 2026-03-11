package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.data.local.TaskDao
import com.arcides.mementoapp.data.local.CategoryDao
import com.arcides.mementoapp.domain.models.Task
import com.arcides.mementoapp.domain.repositories.TaskRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val supabaseClient: SupabaseClient
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> = taskDao.getTasksStream()

    override fun getFilteredTasksStream(
        query: String?,
        status: Task.TaskStatus?,
        priority: Task.Priority?,
        categoryId: String?
    ): Flow<List<Task>> = taskDao.getFilteredTasksStream(query, status, priority, categoryId)

    override suspend fun fetchTasksFromRemote() {
        try {
            val remoteTasks = supabaseClient.postgrest["tasks"]
                .select()
                .decodeList<Task>()

            for (task in remoteTasks) {
                taskDao.insertTask(task)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun createTask(task: Task): String {
        taskDao.insertTask(task)
        try {
            supabaseClient.postgrest["tasks"].insert(task)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (task.categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(task.categoryId, 1)
        }
        return task.id
    }

    override suspend fun updateTask(task: Task) {
        val oldCategoryId = taskDao.getCategoryIdForTask(task.id) ?: ""
        taskDao.updateTask(task)

        try {
            supabaseClient.postgrest["tasks"].update(task) {
                filter {
                    eq("id", task.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (oldCategoryId != task.categoryId) {
            if (oldCategoryId.isNotBlank()) categoryDao.updateTaskCount(oldCategoryId, -1)
            if (task.categoryId.isNotBlank()) categoryDao.updateTaskCount(task.categoryId, 1)
        }
    }

    override suspend fun toggleTaskStatus(taskId: String, newStatus: Task.TaskStatus) {
        val task = taskDao.getTaskById(taskId)
        task?.let { updateTask(it.copy(status = newStatus)) }
    }

    override suspend fun deleteTask(taskId: String) {
        val categoryId = taskDao.getCategoryIdForTask(taskId) ?: ""
        taskDao.deleteById(taskId)
        try {
            supabaseClient.postgrest["tasks"].delete {
                filter {
                    eq("id", taskId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (categoryId.isNotBlank()) categoryDao.updateTaskCount(categoryId, -1)
    }
}

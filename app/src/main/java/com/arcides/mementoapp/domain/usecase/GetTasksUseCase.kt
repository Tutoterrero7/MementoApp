package com.arcides.mementoapp.domain.usecase

import com.arcides.mementoapp.domain.models.Task
import com.arcides.mementoapp.domain.repositories.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(
        query: String = "",
        categoryId: String? = null,
        status: Task.TaskStatus? = null,
        sortByPriority: Boolean = false
    ): Flow<List<Task>> {
        return repository.getTasks().map { tasks ->
            tasks.filter { task ->
                (query.isEmpty() || task.title.contains(query, ignoreCase = true)) &&
                (categoryId == null || task.categoryId == categoryId) &&
                (status == null || task.status == status)
            }.let { filtered ->
                if (sortByPriority) filtered.sortedByDescending { it.priority } else filtered
            }
        }
    }
}

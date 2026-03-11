package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.data.local.CategoryDao
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.repositories.CategoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val supabaseClient: SupabaseClient
) : CategoryRepository {

    override fun getCategoriesStream(): Flow<List<Category>> = categoryDao.getCategoriesStream()

    override suspend fun fetchCategoriesFromRemote() {
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

    override suspend fun createCategory(category: Category): String {
        categoryDao.insertCategory(category)
        try {
            supabaseClient.postgrest["categories"].insert(category)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return category.id
    }

    override suspend fun updateCategory(category: Category) {
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

    override suspend fun deleteCategory(categoryId: String) {
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

    override suspend fun incrementTaskCount(categoryId: String) {
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, 1)
        }
    }

    override suspend fun decrementTaskCount(categoryId: String) {
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, -1)
        }
    }

    override suspend fun updateTaskCount(categoryId: String, delta: Int) {
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, delta)
        }
    }

    override suspend fun hasCategories(): Boolean {
        return categoryDao.getCount() > 0
    }

    override suspend fun createDefaultCategoriesIfNeeded() {
        if (!hasCategories()) {
            val defaultCats = Category.defaultCategories()
            for (category in defaultCats) {
                createCategory(category)
            }
        }
    }
}

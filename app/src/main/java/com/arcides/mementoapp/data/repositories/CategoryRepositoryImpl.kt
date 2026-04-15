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

    override fun getCategoriesStream(userId: String): Flow<List<Category>> = categoryDao.getCategoriesStream(userId)

    override suspend fun fetchCategoriesFromRemote(userId: String) {
        try {
            val remoteCategories = supabaseClient.postgrest["categories"]
                .select {
                    filter {
                        eq("userid", userId)
                    }
                }
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

    override suspend fun deleteCategory(categoryId: String, userId: String) {
        categoryDao.deleteById(categoryId, userId)
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

    override suspend fun incrementTaskCount(categoryId: String, userId: String) {
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, userId, 1)
        }
    }

    override suspend fun decrementTaskCount(categoryId: String, userId: String) {
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, userId, -1)
        }
    }

    override suspend fun updateTaskCount(categoryId: String, userId: String, delta: Int) {
        if (categoryId.isNotBlank()) {
            categoryDao.updateTaskCount(categoryId, userId, delta)
        }
    }

    override suspend fun hasCategories(userId: String): Boolean {
        return categoryDao.getCount(userId) > 0
    }

    override suspend fun createDefaultCategoriesIfNeeded(userId: String) {
        if (!hasCategories(userId)) {
            val defaultCats = Category.defaultCategories(userId)
            for (category in defaultCats) {
                createCategory(category)
            }
        }
    }
}

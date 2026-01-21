package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.domain.models.Category
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun getCategoriesCollection() = firestore
        .collection("users")
        .document(auth.currentUser?.uid ?: throw Exception("Usuario no autenticado"))
        .collection("categories")

    // 1. Obtener categorías en tiempo real
    fun getCategoriesStream(): Flow<List<Category>> = callbackFlow {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = getCategoriesCollection()
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject<Category>()?.copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(categories)
            }

        awaitClose { subscription.remove() }
    }

    // 2. Crear categoría
    suspend fun createCategory(category: Category): String {
        val document = getCategoriesCollection().document()
        val categoryWithId = category.copy(
            id = document.id,
            userId = auth.currentUser?.uid ?: ""
        )

        document.set(categoryWithId).await()
        return document.id
    }

    // 3. Actualizar categoría
    suspend fun updateCategory(category: Category) {
        getCategoriesCollection()
            .document(category.id)
            .set(category)
            .await()
    }

    // 4. Eliminar categoría
    suspend fun deleteCategory(categoryId: String) {
        getCategoriesCollection()
            .document(categoryId)
            .delete()
            .await()
    }

    // 5. Incrementar contador de tareas en categoría
    suspend fun incrementTaskCount(categoryId: String) {
        if (categoryId.isNotBlank()) {
            getCategoriesCollection()
                .document(categoryId)
                .update("taskCount", FieldValue.increment(1))
                .await()
        }
    }

    // 6. Decrementar contador de tareas en categoría
    suspend fun decrementTaskCount(categoryId: String) {
        if (categoryId.isNotBlank()) {
            getCategoriesCollection()
                .document(categoryId)
                .update("taskCount", FieldValue.increment(-1))
                .await()
        }
    }

    // 7. Verificar si el usuario tiene categorías
    suspend fun hasCategories(): Boolean {
        val snapshot = getCategoriesCollection().limit(1).get().await()
        return !snapshot.isEmpty
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
// data/repositories/CategoryRepository.kt
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
                        val data = document.data ?: return@mapNotNull null
                        Category(
                            id = document.id,
                            name = data["name"] as? String ?: "",
                            color = data["color"] as? String ?: "#2196F3",
                            userId = data["userId"] as? String ?: "",
                            taskCount = (data["taskCount"] as? Long ?: 0).toInt()
                        )
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
        val categoryWithId = category.copy(id = document.id)

        val categoryMap = hashMapOf<String, Any>(
            "id" to categoryWithId.id,
            "name" to categoryWithId.name,
            "color" to categoryWithId.color,
            "userId" to auth.currentUser?.uid ?: "",
            "taskCount" to 0
        )

        document.set(categoryMap).await()
        return document.id
    }

    // 3. Actualizar categoría
    suspend fun updateCategory(category: Category) {
        val categoryMap = hashMapOf<String, Any>(
            "name" to category.name,
            "color" to category.color
        )

        getCategoriesCollection()
            .document(category.id)
            .update(categoryMap)
            .await()
    }

    // 4. Eliminar categoría
    suspend fun deleteCategory(categoryId: String) {
        // Primero, actualizar todas las tareas que usan esta categoría
        // (Lo implementaremos después)

        // Luego eliminar la categoría
        getCategoriesCollection()
            .document(categoryId)
            .delete()
            .await()
    }

    // 5. Incrementar contador de tareas
    suspend fun incrementTaskCount(categoryId: String) {
        getCategoriesCollection()
            .document(categoryId)
            .update("taskCount", FieldValue.increment(1))
            .await()
    }

    // 6. Decrementar contador de tareas
    suspend fun decrementTaskCount(categoryId: String) {
        getCategoriesCollection()
            .document(categoryId)
            .update("taskCount", FieldValue.increment(-1))
            .await()
    }
}
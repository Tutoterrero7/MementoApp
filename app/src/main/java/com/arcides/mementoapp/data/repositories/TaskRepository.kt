package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.domain.models.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val categoryRepository: CategoryRepository
) {
    
    // Obtener referencia a la colección del usuario actual
    private fun getTasksCollection() = firestore
        .collection("users")
        .document(auth.currentUser?.uid ?: throw Exception("Usuario no autenticado"))
        .collection("tasks")
    
    // 1. Obtener tareas en tiempo real (Flow)
    fun getTasksStream(): Flow<List<Task>> = callbackFlow {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val subscription = getTasksCollection()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val tasks = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val data = document.data ?: return@mapNotNull null
                        Task(
                            id = document.id,
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            priority = Task.Priority.valueOf(data["priority"] as? String ?: "MEDIUM"),
                            status = Task.TaskStatus.valueOf(data["status"] as? String ?: "PENDING"),
                            categoryId = data["categoryId"] as? String ?: "",
                            userId = data["userId"] as? String ?: "",
                            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(tasks)
            }
        
        awaitClose { subscription.remove() }
    }
    
    // 2. Crear nueva tarea
    suspend fun createTask(task: Task): String {
        val document = getTasksCollection().document()
        val taskWithId = task.copy(id = document.id)
        
        val currentUserId = auth.currentUser?.uid ?: ""
        
        // Convertir a mapa para Firestore
        val taskMap = hashMapOf<String, Any>(
            "id" to taskWithId.id,
            "title" to taskWithId.title,
            "description" to taskWithId.description,
            "priority" to taskWithId.priority.name,
            "status" to taskWithId.status.name,
            "categoryId" to taskWithId.categoryId,
            "userId" to currentUserId,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        document.set(taskMap).await()
        
        // Incrementar contador en categoría si existe
        if (task.categoryId.isNotBlank()) {
            categoryRepository.incrementTaskCount(task.categoryId)
        }
        
        return document.id
    }
    
    // 3. Cambiar estado de tarea
    suspend fun toggleTaskStatus(taskId: String, newStatus: Task.TaskStatus) {
        getTasksCollection()
            .document(taskId)
            .update("status", newStatus.name)
            .await()
    }
    
    // 4. Eliminar tarea
    suspend fun deleteTask(taskId: String) {
        // Primero obtener la tarea para saber su categoría
        val taskDoc = getTasksCollection().document(taskId).get().await()
        val categoryId = taskDoc.get("categoryId") as? String ?: ""
        
        // Eliminar la tarea
        getTasksCollection().document(taskId).delete().await()
        
        // Decrementar contador en categoría si existe
        if (categoryId.isNotBlank()) {
            categoryRepository.decrementTaskCount(categoryId)
        }
    }
    
    // 5. Actualizar categoría de una tarea
    suspend fun updateTaskCategory(taskId: String, newCategoryId: String, oldCategoryId: String) {
        // Actualizar campo en la tarea
        getTasksCollection()
            .document(taskId)
            .update("categoryId", newCategoryId)
            .await()
        
        // Ajustar contadores
        if (oldCategoryId.isNotBlank()) {
            categoryRepository.decrementTaskCount(oldCategoryId)
        }
        if (newCategoryId.isNotBlank()) {
            categoryRepository.incrementTaskCount(newCategoryId)
        }
    }
}
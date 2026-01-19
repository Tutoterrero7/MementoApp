package com.arcides.mementoapp.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Por ahora, implementación vacía
    // La completaremos cuando tengamos Firestore configurado

    suspend fun testConnection(): Boolean {
        return try {
            val currentUser = auth.currentUser
            currentUser != null
        } catch (e: Exception) {
            false
        }
    }
}
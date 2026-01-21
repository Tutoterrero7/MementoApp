package com.arcides.mementoapp.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    // 1. Obtener usuario actual (si hay sesión activa)
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    // 2. Login con email y contraseña
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d("AUTH_REPO", "Firebase login con: $email")
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Log.d("AUTH_REPO", "Login Firebase exitoso: ${result.user?.email}")
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Error Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    // 3. Registro con email y contraseña
    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. Resetear contraseña
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. Cerrar sesión
    fun logout() {
        firebaseAuth.signOut()
    }
}
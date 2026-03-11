package com.arcides.mementoapp.domain.repositories

import com.arcides.mementoapp.domain.models.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String): Result<User>
    suspend fun logout()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(name: String, profilePicture: String?): Result<User>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
}

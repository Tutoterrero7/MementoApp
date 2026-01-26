package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.domain.models.User
import com.arcides.mementoapp.domain.repositories.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<User> {
        // Implementación temporal
        val user = User(id = "1", email = email, name = "Test User")
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun register(email: String, password: String): Result<User> {
        // Implementación temporal
        val user = User(id = "1", email = email, name = "Test User")
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun updateProfile(name: String, profilePicture: String?): Result<User> {
        val currentUserValue = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val updatedUser = currentUserValue.copy(name = name, profilePicture = profilePicture)
        _currentUser.value = updatedUser
        return Result.success(updatedUser)
    }
}

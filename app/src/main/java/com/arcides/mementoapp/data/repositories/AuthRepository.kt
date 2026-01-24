package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.domain.repositories.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    override suspend fun login(email: String, password: String): Result<Unit> {
        // Implementación temporal
        return Result.success(Unit)
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.success(Unit)
    }
}

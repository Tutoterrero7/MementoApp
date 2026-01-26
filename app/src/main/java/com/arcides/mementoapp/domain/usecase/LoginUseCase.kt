package com.arcides.mementoapp.domain.usecase

import com.arcides.mementoapp.domain.models.User
import com.arcides.mementoapp.domain.repositories.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("El email y la contraseña son requeridos"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(Exception("Formato de email inválido"))
        }
        return repository.login(email, password)
    }
}

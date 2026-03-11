package com.arcides.mementoapp.presentation.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.domain.models.User
import com.arcides.mementoapp.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Estados de la UI
    sealed class AuthState {
        data object Idle : AuthState()
        data object Loading : AuthState()
        data class Success(val message: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    // Estado observable del flujo de autenticación
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Usuario actual observable
    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var email: String = ""
    var password: String = ""

    // Validar si el formulario está completo y el email es válido
    val isFormValid: Boolean
        get() = email.isNotBlank() && 
                Patterns.EMAIL_ADDRESS.matcher(email).matches() && 
                password.length >= 6

    // Función de login
    fun login() {
        if (!isFormValid) {
            val errorMsg = if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                "Formato de email inválido"
            } else {
                "La contraseña debe tener al menos 6 caracteres"
            }
            _authState.value = AuthState.Error(errorMsg)
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            
            _authState.value = when {
                result.isSuccess -> AuthState.Success("Login exitoso")
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "Error en login")
            }
        }
    }

    // Función de registro
    fun register() {
        if (!isFormValid) {
            val errorMsg = if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                "Formato de email inválido"
            } else {
                "La contraseña debe tener al menos 6 caracteres"
            }
            _authState.value = AuthState.Error(errorMsg)
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(email, password)

            _authState.value = when {
                result.isSuccess -> AuthState.Success("Cuenta creada exitosamente")
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "Error al registrar")
            }
        }
    }

    // Función para cerrar sesión
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    // RF4: Editar perfil básico
    fun updateProfile(name: String, profilePicture: String? = null) {
        if (name.isBlank()) {
            _authState.value = AuthState.Error("El nombre no puede estar vacío")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.updateProfile(name, profilePicture)
            
            _authState.value = when {
                result.isSuccess -> AuthState.Success("Perfil actualizado correctamente")
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar perfil")
            }
        }
    }

    // RF5: Cambiar contraseña
    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _authState.value = AuthState.Error("La nueva contraseña debe tener al menos 6 caracteres")
            return
        }
        if (newPassword != confirmPassword) {
            _authState.value = AuthState.Error("Las contraseñas no coinciden")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.changePassword(currentPassword, newPassword)
            
            _authState.value = when {
                result.isSuccess -> AuthState.Success("Contraseña cambiada exitosamente")
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "Error al cambiar contraseña")
            }
        }
    }

    // RF18: Recuperar contraseña
    fun resetPassword(email: String) {
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Email inválido")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.resetPassword(email)
            
            _authState.value = when {
                result.isSuccess -> AuthState.Success("Email de recuperación enviado")
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "Error al enviar email")
            }
        }
    }

    // Limpiar estado
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

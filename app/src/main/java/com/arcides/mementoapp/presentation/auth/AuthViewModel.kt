package com.arcides.mementoapp.presentation.auth

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // Estado observable (la UI lo observa)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

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

    // Función para resetear contraseña
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

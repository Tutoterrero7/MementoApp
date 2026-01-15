package com.arcides.mementoapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.data.repositories.AuthRepository
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
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val message: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    // Estado observable (la UI lo observa)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Datos del formulario
    var email = ""
    var password = ""

    // Validar si el formulario está completo
    val isFormValid: Boolean
        get() = email.isNotBlank() && password.length >= 6

    // Función de login
    fun login() {
        if (!isFormValid) {
            _authState.value = AuthState.Error("Email y contraseña requeridos")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)

            _authState.value = when {
                result.isSuccess -> AuthState.Success("Login exitoso")
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    // Función de registro
    fun register() {
        if (!isFormValid) {
            _authState.value = AuthState.Error("Email y contraseña requeridos")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(email, password)

            _authState.value = when {
                result.isSuccess -> AuthState.Success("Cuenta creada exitosamente")
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }
}
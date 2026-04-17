package com.arcides.mementoapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcides.mementoapp.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser = authRepository.currentUser

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun updateName(newName: String) {
        if (newName.isBlank()) {
            _message.value = "El nombre no puede estar vacío"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.updateProfile(newName, null)
            result.onSuccess {
                _message.value = "Perfil actualizado con éxito"
            }.onFailure {
                _message.value = it.message ?: "Error al actualizar perfil"
            }
            _loading.value = false
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _message.value = "Ambas contraseñas son requeridas"
            return
        }

        if (newPassword.length < 6) {
            _message.value = "La nueva contraseña debe tener al menos 6 caracteres"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.changePassword(currentPassword, newPassword)
            result.onSuccess {
                _message.value = "Contraseña cambiada con éxito"
            }.onFailure {
                _message.value = it.message ?: "Error al cambiar la contraseña"
            }
            _loading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

package com.arcides.mementoapp.presentation

/**
 * Interfaz que deben implementar los fragmentos que deseen gestionar la acción
 * del botón flotante central (FAB) de la MainActivity.
 */
interface GlobalActionProvider {
    /**
     * Se llama cuando se hace clic en el botón de acción principal.
     */
    fun onPrimaryActionClicked()
}

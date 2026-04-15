package com.arcides.mementoapp.presentation

import android.app.Application
import com.arcides.mementoapp.utils.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MementoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar el canal de notificaciones al arrancar la app
        NotificationHelper(this)
    }
}
/*
MementoApp es la clase de aplicación general que inicializa Hilt
y fragment_splash.xml es simplemente el diseño de la pantalla de inicio
 */
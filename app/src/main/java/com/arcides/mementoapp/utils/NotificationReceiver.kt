package com.arcides.mementoapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "OnReceive called with action: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Aquí se debería disparar un WorkManager para reprogramar alarmas
            Log.d("NotificationReceiver", "Boot completed received")
            return
        }

        val taskId = intent.getStringExtra("taskId") ?: return
        val title = intent.getStringExtra("title") ?: "Recordatorio de Tarea"
        val message = intent.getStringExtra("message") ?: "Tienes una tarea pendiente"

        Log.d("NotificationReceiver", "Showing notification for task: $taskId")
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(taskId, title, message)
    }
}

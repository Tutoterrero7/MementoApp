package com.arcides.mementoapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.arcides.mementoapp.domain.models.Task
import java.util.Date

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskNotification(task: Task) {
        val dueDate = task.dueDate ?: return
        if (dueDate.before(Date())) return // No programar si ya pasó

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("taskId", task.id)
            putExtra("title", "Recordatorio de Tarea")
            putExtra("message", task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Verificar si tenemos permiso para alarmas exactas (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && 
            !alarmManager.canScheduleExactAlarms()) {
            // Programar de forma aproximada para evitar el crash
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dueDate.time,
                pendingIntent
            )
        } else {
            // Programar exactamente a la hora del vencimiento
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dueDate.time,
                pendingIntent
            )
        }
    }

    fun cancelTaskNotification(taskId: String) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}

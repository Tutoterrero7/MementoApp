package com.arcides.mementoapp.data.local

import androidx.room.TypeConverter
import com.arcides.mementoapp.domain.models.Task
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // Convertidores para Priority Enum
    @TypeConverter
    fun fromPriority(priority: Task.Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(priority: String): Task.Priority {
        return Task.Priority.valueOf(priority)
    }

    // Convertidores para TaskStatus Enum
    @TypeConverter
    fun fromStatus(status: Task.TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(status: String): Task.TaskStatus {
        return Task.TaskStatus.valueOf(status)
    }
}

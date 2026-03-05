package com.arcides.mementoapp.domain.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Serializable
@Parcelize
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val color: String = "#2196F3", // Color en hexadecimal
    val userId: String = "",
    val taskCount: Int = 0,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date()
) : Parcelable {

    companion object {
        // Colores predefinidos (Material Design)
        val DEFAULT_COLORS = listOf(
            "#2196F3", // Blue
            "#4CAF50", // Green
            "#FF9800", // Orange
            "#F44336", // Red
            "#9C27B0", // Purple
            "#00BCD4", // Cyan
            "#8BC34A", // Light Green
            "#FFC107", // Amber
            "#795548", // Brown
            "#607D8B"  // Blue Grey
        )

        // Categorías por defecto
        fun defaultCategories(): List<Category> = listOf(
            Category(name = "Personal", color = "#2196F3"),
            Category(name = "Trabajo", color = "#4CAF50"),
            Category(name = "Estudio", color = "#FF9800"),
            Category(name = "Hogar", color = "#F44336")
        )
    }
}

// domain/models/Category.kt
package com.arcides.mementoapp.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: String = "",
    val name: String = "",
    val color: String = "#2196F3", // Color hexadecimal
    val userId: String = "",
    val taskCount: Int = 0
) : Parcelable {

    // Colores predefinidos (Material Design)
    companion object {
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
    }
}
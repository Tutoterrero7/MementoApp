package com.arcides.mementoapp.domain.models

data class User(
    val id: String,
    val email: String,
    val name: String = "",
    val profilePicture: String? = null
)

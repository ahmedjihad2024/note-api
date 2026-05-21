package com.example.note.user.dto

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val roles: Set<String>,
)

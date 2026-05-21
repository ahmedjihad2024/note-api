package com.example.note.auth.dto

import com.example.note.user.dto.UserResponse

data class AuthResponse(
    val user: UserResponse,
    val accessToken: String,
    val refreshToken: String,
)

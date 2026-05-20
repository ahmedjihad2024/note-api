package com.example.studing.auth.dto

import com.example.studing.user.dto.UserResponse

data class AuthResponse(
    val user: UserResponse,
    val accessToken: String,
    val refreshToken: String,
)

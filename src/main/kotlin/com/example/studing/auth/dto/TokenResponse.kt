package com.example.studing.auth.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

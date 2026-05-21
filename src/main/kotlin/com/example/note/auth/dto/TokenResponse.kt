package com.example.note.auth.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

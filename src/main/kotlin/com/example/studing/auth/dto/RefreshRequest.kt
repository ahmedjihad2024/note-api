package com.example.studing.auth.dto

import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @field:NotBlank(message = "Cannot be blank")
    val refreshToken: String,
)

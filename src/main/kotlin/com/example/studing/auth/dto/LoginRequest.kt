package com.example.studing.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Cannot be blank")
    @field:Email(message = "Invalid format")
    val email: String,

    @field:NotBlank(message = "Cannot be blank")
    val password: String,
)

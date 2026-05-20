package com.example.studing.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank val name: String,
    @field:Email val email: String,
    @field:Size(min = 8, max = 100) val password: String,
)

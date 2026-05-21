package com.example.studing.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.data.mongodb.core.index.Indexed

data class RegisterRequest(
    @field:NotBlank(message = "Cannot be blank")
    @field:Size(min = 2, max = 50, message = "Must be between 2 and 50 characters")
    val name: String,

    @field:NotBlank(message = "Cannot be blank")
    @field:Email(message = "Invalid format")
    val email: String,

    @field:NotBlank(message = "Cannot be blank")
    @field:Size(min = 8, max = 100, message = "Must be between 8 and 100 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+\\$",
        message = "Must contain uppercase, lowercase, a digit, and a special character"
    )
    val password: String,
)

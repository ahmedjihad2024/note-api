package com.example.note.user.dto

import jakarta.validation.constraints.Size

data class UpdateRequest(
    @field:Size(min = 2, max = 50, message = "{validation.name.size}")
    val name: String? = null,
)

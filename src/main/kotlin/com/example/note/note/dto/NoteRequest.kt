package com.example.note.note.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NoteRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,

    @field:NotBlank
    val content: String,

    val color: Long? = null,
)

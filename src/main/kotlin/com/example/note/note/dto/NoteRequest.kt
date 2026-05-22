package com.example.note.note.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size


data class NoteRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(max = 200, message = "{validation.note.title.size}")
    val title: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    val content: String,

    val color: Long? = null,
)

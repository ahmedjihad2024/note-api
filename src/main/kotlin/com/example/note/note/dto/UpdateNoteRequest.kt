package com.example.note.note.dto

import jakarta.validation.constraints.Size

data class UpdateNoteRequest(
    @field:Size(min = 1, max = 200, message = "{validation.note.title.size}")
    val title: String? = null,

    @field:Size(min = 1, message = "{validation.cannot_be_blank}")
    val content: String? = null,

    val color: Long? = null,
)

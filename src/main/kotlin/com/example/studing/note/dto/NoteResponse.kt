package com.example.studing.note.dto

import java.time.Instant

data class NoteResponse(
    val id: String,
    val title: String,
    val content: String,
    val color: Long,
    val createdAt: Instant,
    val updateAt: Instant?
)

package com.example.note.note.mapper

import com.example.note.note.Note
import com.example.note.note.dto.NoteRequest
import com.example.note.note.dto.NoteResponse
import com.example.note.user.entities.User

import java.time.Instant

private const val DEFAULT_COLOR = 0xFFFFFFL

fun Note.toResponse() = NoteResponse(
    id = id,
    title = title,
    content = content,
    color = color,
    createdAt = createdAt,
    updateAt = updatedAt,
)

fun NoteRequest.toEntity(user: User) = Note(
    title = title,
    content = content,
    color = color ?: DEFAULT_COLOR,
    owner = user,
    createdAt = Instant.now(),
)

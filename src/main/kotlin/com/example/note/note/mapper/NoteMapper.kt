package com.example.note.note.mapper

import com.example.note.note.Note
import com.example.note.note.dto.NoteRequest
import com.example.note.note.dto.NoteResponse
import org.bson.types.ObjectId
import java.time.Instant

private const val DEFAULT_COLOR = 0xFFFFFFL

fun Note.toResponse() = NoteResponse(
    id = id.toHexString(),
    title = title,
    content = content,
    color = color,
    createdAt = createdAt,
    updateAt = updateAt,
)

fun NoteRequest.toEntity(ownerId: ObjectId) = Note(
    title = title,
    content = content,
    color = color ?: DEFAULT_COLOR,
    ownerId = ownerId,
    createdAt = Instant.now(),
)

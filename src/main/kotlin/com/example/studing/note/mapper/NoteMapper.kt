package com.example.studing.note.mapper

import com.example.studing.note.Note
import com.example.studing.note.dto.NoteRequest
import com.example.studing.note.dto.NoteResponse
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

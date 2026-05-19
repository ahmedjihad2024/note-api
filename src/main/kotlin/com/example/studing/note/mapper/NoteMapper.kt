package com.example.studing.note.mapper

import com.example.studing.note.entity.Note
import com.example.studing.note.dto.NoteRequest
import com.example.studing.note.dto.NoteResponse
import org.bson.types.ObjectId
import java.time.Instant

fun Note.toResponse() = NoteResponse(
    id = id.toString(),
    title = title,
    content = content,
    color = color,
    createdAt = createdAt,
    updateAt = updateAt
)

fun NoteRequest.toEntity(ownerId: ObjectId) = Note(
    id = id?.let { ObjectId(id) } ?: ObjectId.get(),
    title = title,
    content = content,
    color = color ?: 0xFFFFFFL,
    ownerId = ownerId,
    createdAt = Instant.now()
)

package com.example.studing.note

import com.example.studing.common.exception.ApiException
import com.example.studing.note.dto.NoteRequest
import com.example.studing.note.mapper.toEntity
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class NoteService(
    private val noteRepository: NoteRepository,
) {

    fun create(request: NoteRequest): Note =
        noteRepository.save(request.toEntity(currentUserId()))

    fun listMine(): List<Note> =
        noteRepository.findByOwnerId(currentUserId())

    fun getOwned(noteId: ObjectId): Note {
        val note = noteRepository.findById(noteId).orElseThrow { ApiException.NotFound("Note") }
        if (note.ownerId != currentUserId()) throw ApiException.Forbidden("Not your note.")
        return note
    }

    fun update(noteId: ObjectId, request: NoteRequest): Note {
        val existing = getOwned(noteId)
        return noteRepository.save(
            existing.copy(
                title = request.title,
                content = request.content,
                color = request.color ?: existing.color,
                updateAt = Instant.now(),
            )
        )
    }

    fun delete(noteId: ObjectId) {
        val existing = getOwned(noteId)
        noteRepository.delete(existing)
    }

    private fun currentUserId(): ObjectId {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
            ?: throw ApiException.Unauthorized("error.auth.missing_user")
        
        return ObjectId(principal)
    }
}

package com.example.note.note

import com.example.note.common.exception.ApiException
import com.example.note.note.dto.NoteRequest
import com.example.note.note.dto.UpdateNoteRequest
import com.example.note.note.mapper.toEntity
import com.example.note.user.repository.UserRepository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class NoteService(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository
) {

    fun create(request: NoteRequest): Note {
        if (!userRepository.existsById(currentUserId())) {
            throw ApiException.NotFound("error.user.not_found")
        }
        return noteRepository.save(request.toEntity(userRepository.getReferenceById(currentUserId())))
    }

    fun getNotes(pageable: Pageable): Page<Note> =
        noteRepository.findByOwnerId(currentUserId(), pageable)

    fun getOwned(noteId: Long): Note {
        val note = noteRepository.findById(noteId).orElseThrow { ApiException.NotFound("Note") }
        if (note.owner.id != currentUserId()) throw ApiException.Forbidden("Not your note.")
        return note
    }

    fun update(noteId: Long, request: UpdateNoteRequest): Note {
        val existing = getOwned(noteId)
        return noteRepository.save(
            existing.copy(
                title = request.title ?: existing.title,
                content = request.content ?: existing.content,
                color = request.color ?: existing.color,
                updatedAt = Instant.now(),
            )
        )
    }

    fun delete(noteId: Long) {
        val existing = getOwned(noteId)
        noteRepository.delete(existing)
    }

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? Long
            ?: throw ApiException.Unauthorized("error.auth.missing_user")
        
        return principal
    }
}

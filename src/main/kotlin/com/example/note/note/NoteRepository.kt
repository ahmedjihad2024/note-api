package com.example.note.note


import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository


interface NoteRepository: JpaRepository<Note, Long> {

    fun findByOwnerId(ownerId: Long, pageable: Pageable): Page<Note>

}

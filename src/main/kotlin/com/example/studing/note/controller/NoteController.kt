package com.example.studing.note.controller

import com.example.studing.note.dto.NoteRequest
import com.example.studing.note.dto.NoteResponse
import com.example.studing.note.mapper.toEntity
import com.example.studing.note.mapper.toResponse
import com.example.studing.note.repository.NoteRepository
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val repository: NoteRepository
) {

    @PostMapping
    fun addNote(@Valid @RequestBody body: NoteRequest): NoteResponse {
        val ownerId = ObjectId(body.ownerId)
        return repository.save(body.toEntity(ownerId)).toResponse()
    }


    @GetMapping
    fun getByOwnerId(@RequestParam(required = true) ownerId: String): List<NoteResponse> {
        return repository.findByOwnerId(ObjectId(ownerId)).map { it.toResponse() }
    }

    @DeleteMapping("/{id}")
    fun deleteNote(@PathVariable id: ObjectId) {
        repository.deleteById(id)
    }
}

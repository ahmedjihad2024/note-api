package com.example.studing.note

import com.example.studing.common.dto.ApiResponse
import com.example.studing.note.dto.NoteRequest
import com.example.studing.note.dto.NoteResponse
import com.example.studing.note.mapper.toResponse
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val noteService: NoteService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addNote(@Valid @RequestBody body: NoteRequest): ApiResponse<NoteResponse> =
        ApiResponse.ok(noteService.create(body).toResponse())

    @GetMapping
    fun listMine(): ApiResponse<List<NoteResponse>> =
        ApiResponse.ok(noteService.listMine().map { it.toResponse() })

    @GetMapping("/{id}")
    fun getById(@PathVariable id: ObjectId): ApiResponse<NoteResponse> =
        ApiResponse.ok(noteService.getOwned(id).toResponse())

    @PutMapping("/{id}")
    fun updateNote(
        @PathVariable id: ObjectId,
        @Valid @RequestBody body: NoteRequest,
    ): ApiResponse<NoteResponse> =
        ApiResponse.ok(noteService.update(id, body).toResponse())

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteNote(@PathVariable id: ObjectId) {
        noteService.delete(id)
    }
}

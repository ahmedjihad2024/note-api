package com.example.studing.note

import com.example.studing.common.dto.ApiResponse
import com.example.studing.common.dto.PageResponse
import com.example.studing.note.dto.NoteRequest
import com.example.studing.note.dto.NoteResponse
import com.example.studing.note.mapper.toResponse
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
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
    fun listMine(
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<PageResponse<NoteResponse>> =
        ApiResponse.ok(PageResponse.from(noteService.listMine(pageable)) { it.toResponse() })

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

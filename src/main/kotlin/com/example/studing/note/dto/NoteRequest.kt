package com.example.studing.note.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.bson.types.ObjectId

data class NoteRequest(

    val id: String?,

    @field:NotBlank
    @field:Size(max = 200)
    val title: String,

    @field:NotBlank
    val content: String,

    @field:NotBlank
    var ownerId: String,

    val color: Long? = null,
)

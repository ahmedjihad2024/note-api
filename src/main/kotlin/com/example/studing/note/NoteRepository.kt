package com.example.studing.note

import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface NoteRepository: MongoRepository<Note, ObjectId> {

    fun findByOwnerId(ownerId: ObjectId, pageable: Pageable): Page<Note>

}

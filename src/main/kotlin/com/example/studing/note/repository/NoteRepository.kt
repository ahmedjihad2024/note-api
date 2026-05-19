package com.example.studing.note.repository

import com.example.studing.note.entity.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface NoteRepository: MongoRepository<Note, ObjectId>{

    fun findByOwnerId(ownerId: ObjectId): List<Note>

}

package com.example.studing.user

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("users")
data class User(
    val name: String,
    @Indexed(unique = true) val email: String,
    val hashedPassword: String?,
    @Id val id: ObjectId = ObjectId()
)

package com.example.note.user.mapper

import com.example.note.user.entities.User
import com.example.note.user.dto.UserResponse

fun User.toResponse(): UserResponse = UserResponse(
    id = id.toHexString(),
    name = name,
    email = email,
    roles = roles.map { it.name }.toSet(),
    emailVerified = emailVerified,
)

package com.example.studing.user.mapper

import com.example.studing.user.User
import com.example.studing.user.dto.UserResponse

fun User.toResponse(): UserResponse = UserResponse(
    id = id.toHexString(),
    name = name,
    email = email,
)

package com.example.note.user

import com.example.note.auth.dto.AuthResponse
import com.example.note.common.dto.ApiResponse
import com.example.note.user.dto.UpdateRequest
import com.example.note.user.dto.UserResponse
import com.example.note.user.mapper.toResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService
) {
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal currentUserId: String
    ): ApiResponse<UserResponse> = ApiResponse.ok(userService.me(currentUserId).toResponse())

    @PostMapping
    fun updateUser(
        @Valid @RequestBody body: UpdateRequest,
        @AuthenticationPrincipal currentUserId: String
    ): ApiResponse<UserResponse> = ApiResponse.ok(userService.updateUser(
            currentUserId,
            body.name,
            body.email,
            body.password
        ).toResponse())

}
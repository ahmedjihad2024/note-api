package com.example.studing.auth

import com.example.studing.auth.dto.AuthResponse
import com.example.studing.auth.dto.LoginRequest
import com.example.studing.auth.dto.RefreshRequest
import com.example.studing.auth.dto.RegisterRequest
import com.example.studing.auth.dto.TokenResponse
import com.example.studing.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody body: RegisterRequest): ApiResponse<AuthResponse> =
        ApiResponse.ok(authService.register(body.name, body.email, body.password))

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): ApiResponse<AuthResponse> =
        ApiResponse.ok(authService.login(body.email, body.password))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody body: RefreshRequest): ApiResponse<TokenResponse> =
        ApiResponse.ok(authService.refresh(body.refreshToken))

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @Valid @RequestBody body: RefreshRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authHeader: String?,
    ) {
        val accessToken = authHeader?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        authService.logout(body.refreshToken, accessToken)
    }
}

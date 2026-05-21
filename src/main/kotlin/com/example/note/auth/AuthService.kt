package com.example.note.auth

import com.example.note.auth.dto.AuthResponse
import com.example.note.auth.dto.TokenResponse
import com.example.note.auth.entities.RefreshToken
import com.example.note.auth.entities.RevokedAccessToken
import com.example.note.auth.repository.RefreshTokenRepository
import com.example.note.auth.repository.RevokedAccessTokenRepository
import com.example.note.security.jwt.JwtService
import com.example.note.user.User
import com.example.note.user.UserRepository
import com.example.note.user.mapper.toResponse
import org.bson.types.ObjectId
import com.example.note.common.exception.ApiException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val revokedAccessTokenRepository: RevokedAccessTokenRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
) {

    fun register(name: String, email: String, password: String): AuthResponse {
        if (userRepository.findByEmail(email) != null) {
            throw ApiException.Conflict("error.auth.email_already_exists")
        }
        val user = userRepository.save(
            User(
                name = name,
                email = email,
                hashedPassword = passwordEncoder.encode(password),
            )
        )
        val tokens = issueTokens(user)
        return AuthResponse(user.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    fun login(email: String, password: String): AuthResponse {
        val user = userRepository.findByEmail(email)
            ?: throw ApiException.Unauthorized("error.auth.invalid_email")
        val stored = user.hashedPassword
            ?: throw ApiException.Unauthorized("error.auth.invalid_email")
        if (!passwordEncoder.matches(password, stored)) {
            throw ApiException.Unauthorized("error.auth.invalid_password")
        }
        val tokens = issueTokens(user)
        return AuthResponse(user.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenResponse {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ApiException.Unauthorized("error.auth.invalid_refresh_token")
        }
        val userId = ObjectId(jwtService.getUserIdFromToken(refreshToken))
        val hashed = sha256(refreshToken)

        val stored = refreshTokenRepository.findByUserIdAndHashedToken(userId, hashed)
            ?: throw ApiException.Unauthorized("error.auth.refresh_token_revoked")

        refreshTokenRepository.delete(stored)

        val user = userRepository.findById(userId).orElseThrow {
            ApiException.Unauthorized("error.auth.refresh_token_revoked")
        }
        return issueTokens(user)
    }

    fun logout(refreshToken: String, accessToken: String?) {
        if (jwtService.validateRefreshToken(refreshToken)) {
            val userId = ObjectId(jwtService.getUserIdFromToken(refreshToken))
            refreshTokenRepository.deleteByUserIdAndHashedToken(userId, sha256(refreshToken))
        }
        if (accessToken != null && jwtService.validateAccessToken(accessToken)) {
            revokedAccessTokenRepository.save(
                RevokedAccessToken(
                    jti = jwtService.getJti(accessToken),
                    expiresAt = jwtService.getExpiry(accessToken),
                )
            )
        }
    }

    private fun issueTokens(user: User): TokenResponse {
        val accessToken = jwtService.generateAccessToken(user.id.toHexString(), user.roles)
        val refreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                hashedToken = sha256(refreshToken),
                expiresAt = Instant.now().plus(jwtService.refreshTokenValidityMs, ChronoUnit.MILLIS),
            )
        )
        return TokenResponse(accessToken, refreshToken)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }
}

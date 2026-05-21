package com.example.studing.auth

import com.example.studing.auth.dto.AuthResponse
import com.example.studing.auth.dto.TokenResponse
import com.example.studing.auth.entities.RefreshToken
import com.example.studing.auth.entities.RevokedAccessToken
import com.example.studing.auth.repository.RefreshTokenRepository
import com.example.studing.auth.repository.RevokedAccessTokenRepository
import com.example.studing.security.jwt.JwtService
import com.example.studing.user.User
import com.example.studing.user.UserRepository
import com.example.studing.user.mapper.toResponse
import org.bson.types.ObjectId
import com.example.studing.common.exception.ApiException
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
            throw ApiException.Conflict("A user with that email already exists.")
        }
        val user = userRepository.save(
            User(
                name = name,
                email = email,
                hashedPassword = passwordEncoder.encode(password),
            )
        )
        val tokens = issueTokens(user.id)
        return AuthResponse(user.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    fun login(email: String, password: String): AuthResponse {
        val user = userRepository.findByEmail(email)
            ?: throw ApiException.Unauthorized("Invalid email.")
        val stored = user.hashedPassword
            ?: throw ApiException.Unauthorized("Invalid email.")
        if (!passwordEncoder.matches(password, stored)) {
            throw ApiException.Unauthorized("Invalid password.")
        }
        val tokens = issueTokens(user.id)
        return AuthResponse(user.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenResponse {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ApiException.Unauthorized("Invalid refresh token.")
        }
        val userId = ObjectId(jwtService.getUserIdFromToken(refreshToken))
        val hashed = sha256(refreshToken)

        val stored = refreshTokenRepository.findByUserIdAndHashedToken(userId, hashed)
            ?: throw ApiException.Unauthorized("Refresh token revoked or already used.")

        refreshTokenRepository.delete(stored)
        return issueTokens(userId)
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

    private fun issueTokens(userId: ObjectId): TokenResponse {
        val accessToken = jwtService.generateAccessToken(userId.toHexString())
        val refreshToken = jwtService.generateRefreshToken(userId.toHexString())

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
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

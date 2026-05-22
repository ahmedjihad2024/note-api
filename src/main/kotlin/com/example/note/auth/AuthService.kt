package com.example.note.auth

import com.example.note.auth.dto.AuthResponse
import com.example.note.auth.dto.TokenResponse
import com.example.note.auth.entities.EmailVerificationCode
import com.example.note.auth.entities.RefreshToken
import com.example.note.auth.entities.RevokedAccessToken
import com.example.note.auth.mail.Mailer
import com.example.note.auth.repository.EmailVerificationCodeRepository
import com.example.note.auth.repository.RefreshTokenRepository
import com.example.note.auth.repository.RevokedAccessTokenRepository
import com.example.note.common.exception.ApiException
import com.example.note.common.extentions.tr
import com.example.note.security.jwt.JwtService
import com.example.note.user.User
import com.example.note.user.UserRepository
import com.example.note.user.mapper.toResponse
import org.bson.types.ObjectId
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
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val mailer: Mailer,
) {

    @Transactional
    fun register(name: String, email: String, password: String): AuthResponse.VerificationRequired {
        val existing = userRepository.findByEmail(email)
        if (existing != null) {
            if (existing.emailVerified) {
                throw ApiException.Conflict("error.auth.email_already_exists")
            }
            emailVerificationCodeRepository.deleteByUserId(existing.id)
            userRepository.delete(existing)
        }
        val user = userRepository.save(
            User(
                name = name,
                email = email,
                hashedPassword = passwordEncoder.encode(password),
            )
        )
        issueAndSendVerificationCode(user)
        return AuthResponse.VerificationRequired(
            email = user.email,
            message = "error.auth.verification_code_sent".tr(),
        )
    }

    @Transactional
    fun login(email: String, password: String): AuthResponse {
        val user = userRepository.findByEmail(email)
            ?: throw ApiException.Unauthorized("error.auth.invalid_email")
        val stored = user.hashedPassword
            ?: throw ApiException.Unauthorized("error.auth.invalid_email")
        if (!passwordEncoder.matches(password, stored)) {
            throw ApiException.Unauthorized("error.auth.invalid_password")
        }
        if (!user.emailVerified) {
            issueAndSendVerificationCode(user)
            return AuthResponse.VerificationRequired(
                email = user.email,
                message = "error.auth.verification_code_sent".tr(),
            )
        }
        val tokens = issueTokens(user)
        return AuthResponse.Authenticated(user.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun verifyEmail(email: String, code: String): AuthResponse.Authenticated {
        val user = userRepository.findByEmail(email)
            ?: throw ApiException.NotFound("error.user.not_found")
        if (user.emailVerified) {
            throw ApiException.BadRequest("error.auth.email_already_verified")
        }
        val stored = emailVerificationCodeRepository.findByUserId(user.id)
            ?: throw ApiException.BadRequest("error.auth.verification_code_invalid")
        if (stored.expiresAt.isBefore(Instant.now())) {
            emailVerificationCodeRepository.delete(stored)
            throw ApiException.BadRequest("error.auth.verification_code_expired")
        }
        if (stored.code != code) {
            throw ApiException.BadRequest("error.auth.verification_code_invalid")
        }
        emailVerificationCodeRepository.delete(stored)
        val verified = userRepository.save(
            user.copy(emailVerified = true, emailVerifiedAt = Instant.now())
        )
        val tokens = issueTokens(verified)
        return AuthResponse.Authenticated(verified.toResponse(), tokens.accessToken, tokens.refreshToken)
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

    private fun issueAndSendVerificationCode(user: User) {
        emailVerificationCodeRepository.deleteByUserId(user.id)
        val record = EmailVerificationCode(
            userId = user.id,
            code = STATIC_VERIFICATION_CODE,
            expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES),
        )
        emailVerificationCodeRepository.save(record)
        mailer.sendVerificationCode(user.email, record.code)
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

    companion object {
        private const val STATIC_VERIFICATION_CODE = "12345"
        private const val VERIFICATION_CODE_TTL_MINUTES = 15L
    }
}

package com.example.note.auth

import com.example.note.auth.dto.AuthResponse
import com.example.note.auth.entities.EmailVerificationCode
import com.example.note.auth.entities.RefreshToken
import com.example.note.auth.mail.Mailer
import com.example.note.auth.mail.VerificationCodeGenerator
import com.example.note.auth.passwordReset.PasswordResetCode
import com.example.note.auth.passwordReset.PasswordResetCodeRepository
import com.example.note.auth.repository.EmailVerificationCodeRepository
import com.example.note.auth.repository.RefreshTokenRepository
import com.example.note.auth.repository.RevokedAccessTokenRepository
import com.example.note.common.exception.ApiException
import com.example.note.common.extentions.sha256
import com.example.note.security.jwt.JwtService
import com.example.note.support.Fixtures
import com.example.note.support.initTranslations
import com.example.note.user.entities.User
import com.example.note.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true)
    private val revokedAccessTokenRepository = mockk<RevokedAccessTokenRepository>(relaxed = true)
    private val emailVerificationCodeRepository = mockk<EmailVerificationCodeRepository>(relaxed = true)
    private val passwordResetCodeRepository = mockk<PasswordResetCodeRepository>(relaxed = true)
    private val jwtService = mockk<JwtService>()
    private val passwordEncoder = mockk<org.springframework.security.crypto.password.PasswordEncoder>()
    private val mailer = mockk<Mailer>(relaxed = true)
    private val codeGenerator = mockk<VerificationCodeGenerator>()

    private lateinit var service: AuthService

    private val plainCode = "12345"

    @BeforeEach
    fun setUp() {
        initTranslations()
        service = AuthService(
            userRepository,
            refreshTokenRepository,
            revokedAccessTokenRepository,
            emailVerificationCodeRepository,
            passwordResetCodeRepository,
            jwtService,
            passwordEncoder,
            mailer,
            codeGenerator,
        )

        // Default happy-path stubs; individual tests override what they care about.
        // Spring Data's generic `save(S): S` erases to Object under a relaxed mock, so we
        // echo the argument back with the right concrete type to avoid a cast failure.
        every { userRepository.save(any()) } answers { firstArg() }
        every { refreshTokenRepository.save(any()) } answers { firstArg() }
        every { revokedAccessTokenRepository.save(any()) } answers { firstArg() }
        every { emailVerificationCodeRepository.save(any()) } answers { firstArg() }
        every { passwordResetCodeRepository.save(any()) } answers { firstArg() }
        every { passwordEncoder.encode(any()) } answers { "ENC(${firstArg<String>()})" }
        every { codeGenerator.generate() } returns plainCode
        every { jwtService.generateAccessToken(any(), any()) } returns "access-token"
        every { jwtService.generateRefreshToken(any()) } returns "refresh-token"
        every { jwtService.refreshTokenValidityMs } returns 30L * 24 * 60 * 60 * 1000
    }

    @Nested
    inner class Register {

        @Test
        fun `creates a user, stores a hashed code, and emails it`() {
            every { userRepository.findByEmail("new@example.com") } returns null

            val result = service.register("New", "new@example.com", "secret")

            assertThat(result.email).isEqualTo("new@example.com")
            verify { userRepository.save(match { it.email == "new@example.com" && it.hashedPassword == "ENC(secret)" }) }
            verify { emailVerificationCodeRepository.save(match { it.code == plainCode.sha256() }) }
            verify { mailer.sendVerificationCode("new@example.com", plainCode) }
        }

        @Test
        fun `replaces an existing but unverified account`() {
            val stale = Fixtures.user(email = "x@example.com", emailVerified = false)
            every { userRepository.findByEmail("x@example.com") } returns stale

            service.register("X", "x@example.com", "secret")

            verify { emailVerificationCodeRepository.deleteByUserId(stale.id) }
            verify { userRepository.delete(stale) }
            verify { userRepository.save(any()) }
        }

        @Test
        fun `rejects an email that already belongs to a verified account`() {
            every { userRepository.findByEmail("taken@example.com") } returns
                Fixtures.user(email = "taken@example.com", emailVerified = true)

            assertThatThrownBy { service.register("T", "taken@example.com", "secret") }
                .isInstanceOf(ApiException.Conflict::class.java)

            verify(exactly = 0) { userRepository.save(any()) }
        }
    }

    @Nested
    inner class Login {

        @Test
        fun `rejects an unknown email`() {
            every { userRepository.findByEmail("ghost@example.com") } returns null

            assertThatThrownBy { service.login("ghost@example.com", "pw") }
                .isInstanceOf(ApiException.Unauthorized::class.java)
                .hasMessage("error.auth.invalid_email")
        }

        @Test
        fun `rejects a wrong password`() {
            val user = Fixtures.user()
            every { userRepository.findByEmail(user.email) } returns user
            every { passwordEncoder.matches("wrong", user.hashedPassword!!) } returns false

            assertThatThrownBy { service.login(user.email, "wrong") }
                .isInstanceOf(ApiException.Unauthorized::class.java)
                .hasMessage("error.auth.invalid_password")
        }

        @Test
        fun `re-sends a code instead of tokens when the email is unverified`() {
            val user = Fixtures.user(emailVerified = false)
            every { userRepository.findByEmail(user.email) } returns user
            every { passwordEncoder.matches("pw", user.hashedPassword!!) } returns true

            val result = service.login(user.email, "pw")

            assertThat(result).isInstanceOf(AuthResponse.VerificationRequired::class.java)
            verify { mailer.sendVerificationCode(user.email, plainCode) }
        }

        @Test
        fun `returns tokens for a verified user with the right password`() {
            val user = Fixtures.user(emailVerified = true)
            every { userRepository.findByEmail(user.email) } returns user
            every { passwordEncoder.matches("pw", user.hashedPassword!!) } returns true

            val result = service.login(user.email, "pw") as AuthResponse.Authenticated

            assertThat(result.accessToken).isEqualTo("access-token")
            assertThat(result.refreshToken).isEqualTo("refresh-token")
            assertThat(result.user.email).isEqualTo(user.email)
            verify { refreshTokenRepository.save(any()) }
        }
    }

    @Nested
    inner class VerifyEmail {

        private val user = Fixtures.user(emailVerified = false)

        @Test
        fun `fails when the user is unknown`() {
            every { userRepository.findByEmail("none@example.com") } returns null

            assertThatThrownBy { service.verifyEmail("none@example.com", plainCode) }
                .isInstanceOf(ApiException.NotFound::class.java)
        }

        @Test
        fun `fails when the account is already verified`() {
            every { userRepository.findByEmail(any()) } returns Fixtures.user(emailVerified = true)

            assertThatThrownBy { service.verifyEmail("a@example.com", plainCode) }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.auth.email_already_verified")
        }

        @Test
        fun `rejects and deletes an expired code`() {
            every { userRepository.findByEmail(user.email) } returns user
            val expired = EmailVerificationCode(
                userId = user.id,
                code = plainCode.sha256(),
                expiresAt = Instant.now().minusSeconds(1),
            )
            every { emailVerificationCodeRepository.findByUserId(user.id) } returns expired

            assertThatThrownBy { service.verifyEmail(user.email, plainCode) }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.auth.verification_code_expired")
            verify { emailVerificationCodeRepository.delete(expired) }
        }

        @Test
        fun `rejects a code that does not match`() {
            every { userRepository.findByEmail(user.email) } returns user
            every { emailVerificationCodeRepository.findByUserId(user.id) } returns EmailVerificationCode(
                userId = user.id,
                code = plainCode.sha256(),
                expiresAt = Instant.now().plusSeconds(600),
            )

            assertThatThrownBy { service.verifyEmail(user.email, "99999") }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.auth.verification_code_invalid")
        }

        @Test
        fun `marks the user verified and returns tokens on success`() {
            every { userRepository.findByEmail(user.email) } returns user
            val stored = EmailVerificationCode(
                userId = user.id,
                code = plainCode.sha256(),
                expiresAt = Instant.now().plusSeconds(600),
            )
            every { emailVerificationCodeRepository.findByUserId(user.id) } returns stored

            val result = service.verifyEmail(user.email, plainCode)

            assertThat(result.accessToken).isEqualTo("access-token")
            verify { emailVerificationCodeRepository.delete(stored) }
            verify { userRepository.save(match { it.emailVerified && it.emailVerifiedAt != null }) }
        }
    }

    @Nested
    inner class ResendVerificationCode {

        @Test
        fun `re-issues a code for an existing unverified account`() {
            val user = Fixtures.user(emailVerified = false)
            every { userRepository.findByEmail(user.email) } returns user

            service.resendVerificationCode(user.email)

            verify { mailer.sendVerificationCode(user.email, plainCode) }
        }

        @Test
        fun `stays silent for a verified account but returns the generic message`() {
            val user = Fixtures.user(emailVerified = true)
            every { userRepository.findByEmail(user.email) } returns user

            val result = service.resendVerificationCode(user.email)

            assertThat(result.email).isEqualTo(user.email)
            verify(exactly = 0) { mailer.sendVerificationCode(any(), any()) }
        }

        @Test
        fun `stays silent for an unknown email`() {
            every { userRepository.findByEmail("nobody@example.com") } returns null

            service.resendVerificationCode("nobody@example.com")

            verify(exactly = 0) { mailer.sendVerificationCode(any(), any()) }
        }
    }

    @Nested
    inner class Refresh {

        @Test
        fun `rejects an invalid refresh token`() {
            every { jwtService.validateRefreshToken("bad") } returns false

            assertThatThrownBy { service.refresh("bad") }
                .isInstanceOf(ApiException.Unauthorized::class.java)
                .hasMessage("error.auth.invalid_refresh_token")
        }

        @Test
        fun `rejects a token that is not in the store (already revoked)`() {
            val user = Fixtures.user()
            every { jwtService.validateRefreshToken("rt") } returns true
            every { jwtService.getUserIdFromToken("rt") } returns user.id.toHexString()
            every { refreshTokenRepository.findByUserIdAndHashedToken(user.id, "rt".sha256()) } returns null

            assertThatThrownBy { service.refresh("rt") }
                .isInstanceOf(ApiException.Unauthorized::class.java)
                .hasMessage("error.auth.refresh_token_revoked")
        }

        @Test
        fun `rotates the token and issues a fresh pair on success`() {
            val user = Fixtures.user()
            val stored = RefreshToken(
                userId = user.id,
                hashedToken = "rt".sha256(),
                expiresAt = Instant.now().plusSeconds(60),
            )
            every { jwtService.validateRefreshToken("rt") } returns true
            every { jwtService.getUserIdFromToken("rt") } returns user.id.toHexString()
            every { refreshTokenRepository.findByUserIdAndHashedToken(user.id, "rt".sha256()) } returns stored
            every { userRepository.findById(user.id) } returns Optional.of(user)

            val result = service.refresh("rt")

            assertThat(result.accessToken).isEqualTo("access-token")
            assertThat(result.refreshToken).isEqualTo("refresh-token")
            verify { refreshTokenRepository.delete(stored) }
            verify { refreshTokenRepository.save(any()) }
        }
    }

    @Nested
    inner class Logout {

        @Test
        fun `deletes the refresh token and revokes the access token`() {
            val user = Fixtures.user()
            every { jwtService.validateRefreshToken("rt") } returns true
            every { jwtService.getUserIdFromToken("rt") } returns user.id.toHexString()
            every { jwtService.validateAccessToken("at") } returns true
            every { jwtService.getJti("at") } returns "jti-1"
            every { jwtService.getExpiry("at") } returns Instant.now().plusSeconds(600)

            service.logout("rt", "at")

            verify { refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, "rt".sha256()) }
            verify { revokedAccessTokenRepository.save(match { it.jti == "jti-1" }) }
        }

        @Test
        fun `is a no-op when both tokens are invalid`() {
            every { jwtService.validateRefreshToken("rt") } returns false
            every { jwtService.validateAccessToken("at") } returns false

            service.logout("rt", "at")

            verify(exactly = 0) { refreshTokenRepository.deleteByUserIdAndHashedToken(any(), any()) }
            verify(exactly = 0) { revokedAccessTokenRepository.save(any()) }
        }
    }

    @Nested
    inner class ForgotPassword {

        @Test
        fun `stores and emails a reset code when the user exists`() {
            val user = Fixtures.user()
            every { userRepository.findByEmail(user.email) } returns user

            service.forgotPassword(user.email)

            verify { passwordResetCodeRepository.deleteByUserId(user.id) }
            verify { passwordResetCodeRepository.save(match { it.code == plainCode.sha256() }) }
            verify { mailer.sendVerificationCode(user.email, plainCode) }
        }

        @Test
        fun `does nothing observable for an unknown email`() {
            every { userRepository.findByEmail("nobody@example.com") } returns null

            service.forgotPassword("nobody@example.com")

            verify(exactly = 0) { passwordResetCodeRepository.save(any()) }
            verify(exactly = 0) { mailer.sendVerificationCode(any(), any()) }
        }
    }

    @Nested
    inner class VerifyResetCode {

        private val user = Fixtures.user()

        @Test
        fun `accepts a matching, unexpired code`() {
            every { userRepository.findByEmail(user.email) } returns user
            every { passwordResetCodeRepository.findByUserId(user.id) } returns PasswordResetCode(
                userId = user.id,
                code = plainCode.sha256(),
                expiresAt = Instant.now().plusSeconds(600),
            )

            service.verifyResetCode(user.email, plainCode) // does not throw
        }

        @Test
        fun `rejects an expired code`() {
            every { userRepository.findByEmail(user.email) } returns user
            every { passwordResetCodeRepository.findByUserId(user.id) } returns PasswordResetCode(
                userId = user.id,
                code = plainCode.sha256(),
                expiresAt = Instant.now().minusSeconds(1),
            )

            assertThatThrownBy { service.verifyResetCode(user.email, plainCode) }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.auth.password_reset_code_expired")
        }

        @Test
        fun `rejects a wrong code`() {
            every { userRepository.findByEmail(user.email) } returns user
            every { passwordResetCodeRepository.findByUserId(user.id) } returns PasswordResetCode(
                userId = user.id,
                code = plainCode.sha256(),
                expiresAt = Instant.now().plusSeconds(600),
            )

            assertThatThrownBy { service.verifyResetCode(user.email, "00000") }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.auth.password_reset_code_invalid")
        }
    }

    @Nested
    inner class ResetPassword {

        private val user = Fixtures.user()

        @Test
        fun `re-hashes the password, clears refresh tokens, and returns new tokens`() {
            val stored = PasswordResetCode(
                userId = user.id,
                code = plainCode.sha256(),
                expiresAt = Instant.now().plusSeconds(600),
            )
            every { userRepository.findByEmail(user.email) } returns user
            every { passwordResetCodeRepository.findByUserId(user.id) } returns stored

            val result = service.resetPassword(user.email, plainCode, "brand-new")

            assertThat(result.accessToken).isEqualTo("access-token")
            verify { userRepository.save(match { it.hashedPassword == "ENC(brand-new)" }) }
            verify { passwordResetCodeRepository.delete(stored) }
            verify { refreshTokenRepository.deleteAllByUserId(user.id) }
        }

        @Test
        fun `rejects an expired code and deletes it`() {
            val expired = PasswordResetCode(
                userId = user.id,
                code = plainCode.sha256(),
                expiresAt = Instant.now().minusSeconds(1),
            )
            every { userRepository.findByEmail(user.email) } returns user
            every { passwordResetCodeRepository.findByUserId(user.id) } returns expired

            assertThatThrownBy { service.resetPassword(user.email, plainCode, "x") }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.auth.password_reset_code_expired")
            verify { passwordResetCodeRepository.delete(expired) }
        }
    }
}

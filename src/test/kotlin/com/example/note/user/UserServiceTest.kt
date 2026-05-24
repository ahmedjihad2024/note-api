package com.example.note.user

import com.example.note.auth.AuthService
import com.example.note.auth.dto.AuthResponse
import com.example.note.auth.mail.Mailer
import com.example.note.auth.mail.VerificationCodeGenerator
import com.example.note.common.exception.ApiException
import com.example.note.common.extentions.sha256
import com.example.note.support.Fixtures
import com.example.note.support.initTranslations
import com.example.note.user.entities.EmailChangeRequest
import com.example.note.user.entities.User
import com.example.note.user.mapper.toResponse
import com.example.note.user.repository.EmailChangeRequestRepository
import com.example.note.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.Optional

class UserServiceTest {

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val emailChangeRequestRepository = mockk<EmailChangeRequestRepository>(relaxed = true)
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val authService = mockk<AuthService>()
    private val mailer = mockk<Mailer>(relaxed = true)
    private val avatarStorage = mockk<AvatarStorage>(relaxed = true)
    private val codeGenerator = mockk<VerificationCodeGenerator>()

    private lateinit var service: UserService

    @BeforeEach
    fun setUp() {
        initTranslations()
        service = UserService(
            userRepository,
            emailChangeRequestRepository,
            passwordEncoder,
            authService,
            mailer,
            avatarStorage,
            codeGenerator,
        )
        // Echo saved entities back with their concrete type (a relaxed mock would erase
        // Spring Data's generic `save(S): S` to Object and fail the implicit cast).
        every { userRepository.save(any()) } answers { firstArg() }
        every { emailChangeRequestRepository.save(any()) } answers { firstArg() }
        every { passwordEncoder.encode(any()) } answers { "ENC(${firstArg<String>()})" }
        every { codeGenerator.generate() } returns "12345"
    }

    @Nested
    inner class Me {

        @Test
        fun `returns the user when found`() {
            val user = Fixtures.user()
            every { userRepository.findById(user.id) } returns Optional.of(user)

            assertThat(service.me(user.id.toHexString())).isSameAs(user)
        }

        @Test
        fun `throws NotFound when missing`() {
            val user = Fixtures.user()
            every { userRepository.findById(user.id) } returns Optional.empty()

            assertThatThrownBy { service.me(user.id.toHexString()) }
                .isInstanceOf(ApiException.NotFound::class.java)
        }
    }

    @Nested
    inner class AvatarUrl {

        @BeforeEach
        fun bindRequest() {
            RequestContextHolder.setRequestAttributes(ServletRequestAttributes(MockHttpServletRequest()))
        }

        @AfterEach
        fun clearRequest() {
            RequestContextHolder.resetRequestAttributes()
        }

        @Test
        fun `is null when the user has no avatar`() {
            assertThat(service.avatarUrl(Fixtures.user(avatarFilename = null))).isNull()
        }

        @Test
        fun `points at the avatars endpoint when set`() {
            val url = service.avatarUrl(Fixtures.user(avatarFilename = "abc.png"))

            assertThat(url).endsWith("/avatars/abc.png")
        }
    }

    @Nested
    inner class UploadAvatar {

        @Test
        fun `stores the new file, persists the pointer, then deletes the old file`() {
            val user = Fixtures.user(avatarFilename = "old.png")
            val file = mockk<MultipartFile>()
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { avatarStorage.store(file) } returns "new.png"

            val result = service.uploadAvatar(user.id.toHexString(), file)

            assertThat(result.avatarFilename).isEqualTo("new.png")
            verifyOrder {
                userRepository.save(match { it.avatarFilename == "new.png" })
                avatarStorage.delete("old.png")
            }
        }

        @Test
        fun `does not delete anything when there was no previous avatar`() {
            val user = Fixtures.user(avatarFilename = null)
            val file = mockk<MultipartFile>()
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { avatarStorage.store(file) } returns "new.png"

            service.uploadAvatar(user.id.toHexString(), file)

            verify(exactly = 0) { avatarStorage.delete(any()) }
        }
    }

    @Nested
    inner class UpdateName {

        @Test
        fun `persists a new name`() {
            val user = Fixtures.user(name = "Old")
            every { userRepository.findById(user.id) } returns Optional.of(user)

            val result = service.updateName(user.id.toHexString(), "New")

            assertThat(result.name).isEqualTo("New")
            verify { userRepository.save(match { it.name == "New" }) }
        }

        @Test
        fun `is a no-op when name is null`() {
            val user = Fixtures.user(name = "Old")
            every { userRepository.findById(user.id) } returns Optional.of(user)

            val result = service.updateName(user.id.toHexString(), null)

            assertThat(result.name).isEqualTo("Old")
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }

    @Nested
    inner class ChangePassword {

        @Test
        fun `rejects a wrong current password`() {
            val user = Fixtures.user()
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { passwordEncoder.matches("wrong", user.hashedPassword!!) } returns false

            assertThatThrownBy { service.changePassword(user.id.toHexString(), "wrong", "new") }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.user.invalid_current_password")
        }

        @Test
        fun `re-hashes and reissues tokens on success`() {
            val user = Fixtures.user()
            val reissued = AuthResponse.Authenticated(
                user = user.toResponse(),
                accessToken = "a",
                refreshToken = "r",
            )
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { passwordEncoder.matches("current", user.hashedPassword!!) } returns true
            val saved = slot<User>()
            every { userRepository.save(capture(saved)) } answers { saved.captured }
            every { authService.reissueAfterPasswordChange(any()) } returns reissued

            val result = service.changePassword(user.id.toHexString(), "current", "new")

            assertThat(result).isSameAs(reissued)
            assertThat(saved.captured.hashedPassword).isEqualTo("ENC(new)")
            verify { authService.reissueAfterPasswordChange(any()) }
        }
    }

    @Nested
    inner class ChangeEmail {

        @Test
        fun `rejects changing to the current email`() {
            val user = Fixtures.user(email = "me@example.com")
            every { userRepository.findById(user.id) } returns Optional.of(user)

            assertThatThrownBy { service.changeEmail(user.id.toHexString(), "me@example.com") }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.user.email_same_as_current")
        }

        @Test
        fun `rejects an email already taken by another user`() {
            val user = Fixtures.user(email = "me@example.com")
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { userRepository.findByEmail("taken@example.com") } returns Fixtures.user(email = "taken@example.com")

            assertThatThrownBy { service.changeEmail(user.id.toHexString(), "taken@example.com") }
                .isInstanceOf(ApiException.Conflict::class.java)
        }

        @Test
        fun `stores a pending change and emails the new address`() {
            val user = Fixtures.user(email = "me@example.com")
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { userRepository.findByEmail("next@example.com") } returns null

            val result = service.changeEmail(user.id.toHexString(), "next@example.com")

            assertThat(result.email).isEqualTo("next@example.com")
            verify { emailChangeRequestRepository.save(match { it.newEmail == "next@example.com" && it.code == "12345".sha256() }) }
            verify { mailer.sendVerificationCode("next@example.com", "12345") }
        }
    }

    @Nested
    inner class VerifyChangeEmailCode {

        private val user = Fixtures.user(email = "me@example.com")

        @Test
        fun `fails when there is no pending change`() {
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { emailChangeRequestRepository.findByUserId(user.id) } returns null

            assertThatThrownBy { service.verifyChangeEmailCode(user.id.toHexString(), "12345") }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.user.no_pending_email_change")
        }

        @Test
        fun `rejects and deletes an expired request`() {
            val pending = pending(expiresAt = Instant.now().minusSeconds(1))
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { emailChangeRequestRepository.findByUserId(user.id) } returns pending

            assertThatThrownBy { service.verifyChangeEmailCode(user.id.toHexString(), "12345") }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.user.email_change_code_expired")
            verify { emailChangeRequestRepository.delete(pending) }
        }

        @Test
        fun `rejects a wrong code`() {
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { emailChangeRequestRepository.findByUserId(user.id) } returns pending()

            assertThatThrownBy { service.verifyChangeEmailCode(user.id.toHexString(), "00000") }
                .isInstanceOf(ApiException.BadRequest::class.java)
                .hasMessage("error.user.email_change_code_invalid")
        }

        @Test
        fun `applies the new email on success`() {
            every { userRepository.findById(user.id) } returns Optional.of(user)
            val pending = pending()
            every { emailChangeRequestRepository.findByUserId(user.id) } returns pending
            every { userRepository.findByEmail("next@example.com") } returns null

            val result = service.verifyChangeEmailCode(user.id.toHexString(), "12345")

            assertThat(result.email).isEqualTo("next@example.com")
            verify { emailChangeRequestRepository.delete(pending) }
        }

        private fun pending(expiresAt: Instant = Instant.now().plusSeconds(600)) = EmailChangeRequest(
            userId = user.id,
            newEmail = "next@example.com",
            code = "12345".sha256(),
            expiresAt = expiresAt,
        )
    }
}

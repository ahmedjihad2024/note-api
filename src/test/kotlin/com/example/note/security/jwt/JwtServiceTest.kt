package com.example.note.security.jwt

import com.example.note.user.enums.Role
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

class JwtServiceTest {

    private val secret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    private val jwtService = JwtService(secret)

    // A second service with an unrelated key, used to prove signatures are verified.
    private val otherService = JwtService(
        Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
    )

    private val userId = "65f0c0c0c0c0c0c0c0c0c0c0"

    @Nested
    inner class AccessTokens {

        @Test
        fun `validate as access and carry subject plus roles`() {
            val token = jwtService.generateAccessToken(userId, setOf(Role.USER, Role.ADMIN))

            assertThat(jwtService.validateAccessToken(token)).isTrue()
            assertThat(jwtService.validateRefreshToken(token)).isFalse()
            assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(userId)
            assertThat(jwtService.getRolesFromToken(token)).containsExactlyInAnyOrder(Role.USER, Role.ADMIN)
        }

        @Test
        fun `expire roughly fifteen minutes out`() {
            val token = jwtService.generateAccessToken(userId, setOf(Role.USER))

            val expiry = jwtService.getExpiry(token)
            assertThat(expiry).isCloseTo(
                Instant.now().plus(15, ChronoUnit.MINUTES),
                org.assertj.core.api.Assertions.within(30, ChronoUnit.SECONDS),
            )
        }

        @Test
        fun `carry a unique jti per token`() {
            val first = jwtService.generateAccessToken(userId, setOf(Role.USER))
            val second = jwtService.generateAccessToken(userId, setOf(Role.USER))

            assertThat(jwtService.getJti(first)).isNotBlank()
            assertThat(jwtService.getJti(first)).isNotEqualTo(jwtService.getJti(second))
        }
    }

    @Nested
    inner class RefreshTokens {

        @Test
        fun `validate as refresh and expose no roles`() {
            val token = jwtService.generateRefreshToken(userId)

            assertThat(jwtService.validateRefreshToken(token)).isTrue()
            assertThat(jwtService.validateAccessToken(token)).isFalse()
            assertThat(jwtService.getRolesFromToken(token)).isEmpty()
        }
    }

    @Nested
    inner class InvalidTokens {

        @Test
        fun `garbage tokens never validate`() {
            assertThat(jwtService.validateAccessToken("not-a-jwt")).isFalse()
            assertThat(jwtService.validateRefreshToken("not-a-jwt")).isFalse()
        }

        @Test
        fun `tokens signed with another key are rejected`() {
            val foreign = otherService.generateAccessToken(userId, setOf(Role.USER))

            assertThat(jwtService.validateAccessToken(foreign)).isFalse()
        }

        @Test
        fun `reading the subject from an invalid token throws`() {
            assertThatThrownBy { jwtService.getUserIdFromToken("garbage") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `strips a leading Bearer prefix before parsing`() {
        val token = jwtService.generateAccessToken(userId, setOf(Role.USER))

        assertThat(jwtService.validateAccessToken("Bearer $token")).isTrue()
        assertThat(jwtService.getUserIdFromToken("Bearer $token")).isEqualTo(userId)
    }
}

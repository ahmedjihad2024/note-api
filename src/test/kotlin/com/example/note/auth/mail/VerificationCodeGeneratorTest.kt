package com.example.note.auth.mail

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VerificationCodeGeneratorTest {

    @Test
    fun `random generator always returns a zero-padded five-digit code`() {
        val generator = RandomVerificationCodeGenerator()

        repeat(1_000) {
            assertThat(generator.generate()).matches("\\d{5}")
        }
    }
}

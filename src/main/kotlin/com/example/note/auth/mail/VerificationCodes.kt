package com.example.note.auth.mail

import java.security.SecureRandom

const val VERIFICATION_CODE_TTL_MINUTES: Long = 15L

private val rng = SecureRandom()

// 5-digit code (00000..99999). Range matches the DTO regex `^\\d{5}$`.
fun generateVerificationCode(): String =
    "%05d".format(rng.nextInt(100_000))

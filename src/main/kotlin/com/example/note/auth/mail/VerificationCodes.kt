package com.example.note.auth.mail

// Static placeholder used while the verification flow is being tested. Swap to
// `Random.nextInt(10000, 100000).toString()` (and store SHA-256 of it the way
// RefreshToken does) before this ships to real users.
const val STATIC_VERIFICATION_CODE: String = "12345"

const val VERIFICATION_CODE_TTL_MINUTES: Long = 15L

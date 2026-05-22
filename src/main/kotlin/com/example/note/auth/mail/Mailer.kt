package com.example.note.auth.mail

interface Mailer {
    fun sendVerificationCode(email: String, code: String)
}

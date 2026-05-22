package com.example.note.auth.mail

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LogMailer : Mailer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(email: String, code: String) {
        log.info("[Mailer] Verification code for {} = {}", email, code)
    }
}

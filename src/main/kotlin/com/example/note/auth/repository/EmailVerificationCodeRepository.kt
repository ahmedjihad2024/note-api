package com.example.note.auth.repository

import com.example.note.auth.entities.EmailVerificationCode
import org.springframework.data.jpa.repository.JpaRepository

interface EmailVerificationCodeRepository : JpaRepository<EmailVerificationCode, Long> {

    fun findByUserId(userId: Long): EmailVerificationCode?
    fun deleteByUserId(userId: Long)
}

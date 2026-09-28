package com.example.note.auth.passwordReset

import org.springframework.data.jpa.repository.JpaRepository

interface PasswordResetCodeRepository : JpaRepository<PasswordResetCode, Long> {
    fun findByUserId(userId: Long): PasswordResetCode?
    fun deleteByUserId(userId: Long)
}
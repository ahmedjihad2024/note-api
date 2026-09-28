package com.example.note.auth.repository

import com.example.note.auth.entities.RevokedAccessToken
import org.springframework.data.jpa.repository.JpaRepository

interface RevokedAccessTokenRepository : JpaRepository<RevokedAccessToken, Long> {
    fun existsByJti(jti: String): Boolean
}

package com.example.note.auth.repository

import com.example.note.auth.entities.RefreshToken

import org.springframework.data.jpa.repository.JpaRepository


interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByUserIdAndHashedToken(userId: Long, hashedToken: String): RefreshToken?
    fun deleteByUserIdAndHashedToken(userId: Long, hashedToken: String)
    fun deleteAllByUserId(userId: Long)
}

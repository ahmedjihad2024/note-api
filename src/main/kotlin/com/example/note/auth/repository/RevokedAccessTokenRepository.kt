package com.example.note.auth.repository

import com.example.note.auth.entities.RevokedAccessToken
import org.springframework.data.mongodb.repository.MongoRepository

interface RevokedAccessTokenRepository : MongoRepository<RevokedAccessToken, String> {
    fun existsByJti(jti: String): Boolean
}

package com.example.studing.auth.repository

import com.example.studing.auth.entities.RevokedAccessToken
import org.springframework.data.mongodb.repository.MongoRepository

interface RevokedAccessTokenRepository : MongoRepository<RevokedAccessToken, String> {
    fun existsByJti(jti: String): Boolean
}

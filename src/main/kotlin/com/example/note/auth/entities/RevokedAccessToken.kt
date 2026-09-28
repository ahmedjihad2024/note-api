package com.example.note.auth.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import java.time.Instant

@Entity
@DynamicUpdate
@Table(name = "revoked_access_tokens")
data class RevokedAccessToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, unique = true)
    val jti: String,

    @Column(nullable = false)
    val expiresAt: Instant,
)
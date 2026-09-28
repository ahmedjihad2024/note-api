package com.example.note.auth.entities

import com.example.note.user.entities.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import jakarta.persistence.Index
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "user_id_hashed_token_idx", columnList = "user_id, hashed_token")
    ]
)
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "hashed_token", nullable = false)
    val hashedToken: String,

    @Column(nullable = false)
    @CreationTimestamp
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val expiresAt: Instant,
)
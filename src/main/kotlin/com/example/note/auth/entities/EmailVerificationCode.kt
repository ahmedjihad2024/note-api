package com.example.note.auth.entities

import com.example.note.user.entities.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.DynamicUpdate
import java.time.Instant

@Entity
@Table(name = "email_verification_codes")
@DynamicUpdate
data class EmailVerificationCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @OneToOne
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(nullable = false)
    val code: String,

    @Column(nullable = false)
    @CreationTimestamp
    val createdAt: Instant = Instant.now(),

    // TODO: add a schedule to remove expired verification codes
    @Column(nullable = false)
    val expiresAt: Instant,
)
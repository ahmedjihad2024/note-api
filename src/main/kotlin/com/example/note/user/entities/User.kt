package com.example.note.user.entities

import com.example.note.user.enums.Role
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import jakarta.persistence.Index
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email")
    ]
)
data class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "hashed_password")
    var hashedPassword: String?,

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "role")
    val roles: Set<Role> = setOf(Role.USER),

    @Column(nullable = false)
    val emailVerified: Boolean = false,

    val emailVerifiedAt: Instant? = null,

    // On-disk filename of the profile picture (e.g. "<uuid>.png");
    // null when the user has no avatar.
    val avatarFilename: String? = null,
)
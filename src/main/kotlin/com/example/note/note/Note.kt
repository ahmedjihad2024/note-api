package com.example.note.note

import com.example.note.user.entities.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import jakarta.persistence.Index
import org.hibernate.annotations.DynamicUpdate


@Entity
@DynamicUpdate
@Table(
    name = "notes",
    indexes = [
        Index(name = "idx_notes_owner_id", columnList = "owner_id")
    ]
)
data class Note(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false)
    val color: Long,

    @Column(nullable = false)
    val createdAt: Instant,

    val updatedAt: Instant? = null,

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,
)
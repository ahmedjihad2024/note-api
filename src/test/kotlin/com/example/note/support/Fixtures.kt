package com.example.note.support

import com.example.note.note.Note
import com.example.note.user.entities.User
import com.example.note.user.enums.Role
import org.bson.types.ObjectId
import java.time.Instant

/**
 * Central builders for domain entities used across unit tests. Each builder fills in
 * sensible defaults so a test only has to name the fields it actually cares about.
 */
object Fixtures {

    fun user(
        id: ObjectId = ObjectId(),
        name: String = "Ada Lovelace",
        email: String = "ada@example.com",
        hashedPassword: String? = "hashed-password",
        roles: Set<Role> = setOf(Role.USER),
        emailVerified: Boolean = true,
        avatarFilename: String? = null,
    ): User = User(
        id = id,
        name = name,
        email = email,
        hashedPassword = hashedPassword,
        roles = roles,
        emailVerified = emailVerified,
        avatarFilename = avatarFilename,
    )

    fun note(
        id: ObjectId = ObjectId(),
        ownerId: ObjectId = ObjectId(),
        title: String = "Title",
        content: String = "Content",
        color: Long = 0xFFFFFFL,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        updateAt: Instant? = null,
    ): Note = Note(
        id = id,
        ownerId = ownerId,
        title = title,
        content = content,
        color = color,
        createdAt = createdAt,
        updateAt = updateAt,
    )
}

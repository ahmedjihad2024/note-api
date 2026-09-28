package com.example.note.user.repository

import com.example.note.user.entities.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository: JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByName(name: String): User?
}
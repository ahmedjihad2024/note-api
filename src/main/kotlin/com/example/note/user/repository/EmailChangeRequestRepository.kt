package com.example.note.user.repository

import com.example.note.user.entities.EmailChangeRequest

import org.springframework.data.jpa.repository.JpaRepository


interface EmailChangeRequestRepository : JpaRepository<EmailChangeRequest, Long> {
    fun findByUserId(userId: Long): EmailChangeRequest?
    fun deleteByUserId(userId: Long)
}

package com.example.note.user

import com.example.note.common.exception.ApiException
import jakarta.validation.constraints.Email
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun me(id: String): User {
        return userRepository.findById(ObjectId(id))
            .orElseGet { throw ApiException.NotFound("error.user.not_found") }
    }

    @Transactional
    fun updateUser(id: String, name: String?, email: String?, password: String?): User {
        var user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        user = user.copy(
            name = name ?: user.name,
            email = email ?: user.email,
            password?.let { passwordEncoder.encode(it) } ?: user.hashedPassword
        )
        return userRepository.save(user)
    }
}
package com.example.studing.user

enum class Role {
    USER,
    ADMIN;

    fun authority(): String = "ROLE_$name"

    companion object {
        fun fromAuthority(authority: String): Role? =
            entries.firstOrNull { it.authority() == authority }
    }
}

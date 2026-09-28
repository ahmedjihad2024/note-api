package com.example.note.config

import org.springframework.boot.health.contributor.AbstractHealthIndicator
import org.springframework.boot.health.contributor.Health
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component("postgresHealthIndicator")
class PostgresConnectivityHealthIndicator(
    private val jdbcTemplate: JdbcTemplate,
) : AbstractHealthIndicator() {

    override fun doHealthCheck(builder: Health.Builder) {
        jdbcTemplate.queryForObject("SELECT 1", Int::class.java)

        builder
            .up()
            .withDetail("database", "PostgreSQL")
    }
}
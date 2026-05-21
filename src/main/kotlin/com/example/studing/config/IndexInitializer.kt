package com.example.studing.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class IndexInitializer(
    private val mongoTemplate: MongoTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun ensureIndexes() {
        log.info("Ensuring MongoDB indexes...")

        mongoTemplate.indexOps("refresh_tokens").createIndex(
            Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS)
        )

        mongoTemplate.indexOps("revoked_access_tokens").createIndex(
            Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS)
        )

        mongoTemplate.indexOps("users").createIndex(
            Index().on("email", Sort.Direction.ASC).unique()
        )

        log.info("MongoDB indexes ensured.")
    }
}

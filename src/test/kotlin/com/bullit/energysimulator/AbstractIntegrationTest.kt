package com.bullit.energysimulator

import org.slf4j.LoggerFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
@ActiveProfiles(profiles = ["test"])
abstract class AbstractIntegrationTest() {
    companion object {
        private val logger = LoggerFactory.getLogger(AbstractIntegrationTest::class.java)

        @Container
        val postgres = PostgreSQLContainer("postgres:16.2")

        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            logger.info("Port mapped: ${postgres.firstMappedPort}")
            logger.info("Username: ${postgres.username}")
            logger.info("Password: ${postgres.password}")
            logger.info("Database: ${postgres.databaseName}")
            registry.add("spring.r2dbc.url") { "r2dbc:postgresql://localhost:${postgres.firstMappedPort}/${postgres.databaseName}" }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.liquibase.url") { "jdbc:postgresql://localhost:${postgres.firstMappedPort}/${postgres.databaseName}" }
            registry.add("spring.liquibase.user") { postgres.username }
            registry.add("spring.liquibase.password") { postgres.password }
        }
    }
}


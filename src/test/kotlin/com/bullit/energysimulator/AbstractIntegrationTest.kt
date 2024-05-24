package com.bullit.energysimulator

import org.slf4j.LoggerFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
@ActiveProfiles(profiles = ["test"])
abstract class AbstractIntegrationTest {
    companion object {
        private val logger = LoggerFactory.getLogger(AbstractIntegrationTest::class.java)

        @Container
        val elastic = ElasticsearchContainer("elasticsearch:8.13.0").apply {
            withEnv("discovery.type", "single-node")
            withEnv("xpack.security.enabled", "false")
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            logger.info("Elastic port: ${elastic.firstMappedPort}")
            registry.add("elasticsearch.url") { "localhost:${elastic.firstMappedPort}" }
        }
    }
}


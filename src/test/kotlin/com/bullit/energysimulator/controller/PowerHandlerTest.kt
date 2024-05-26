package com.bullit.energysimulator.controller

import com.bullit.energysimulator.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@DirtiesContext
@SpringBootTest
class PowerHandlerTest : AbstractIntegrationTest() {

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun initClient(context: ApplicationContext) {
        webTestClient = WebTestClient
            .bindToApplicationContext(context)
            .configureClient()
            .responseTimeout(300.seconds.toJavaDuration())
            .build()
    }

    @Test
    fun `should handle a power csv with a fixed contract`() {
        webTestClient
            .get()
            .uri("/import/power?source=fixed")
            .exchange()
            .expectStatus().isOk
            .expectBody(AccumulatedConsumptionDTO::class.java)
            .consumeWith { response ->
                assertEquals(2, response.responseBody?.accumulatedConsumptions?.size)
                assertEquals(3, response.responseBody?.accumulatedConsumptions?.first()?.month)
                assertEquals(2024, response.responseBody?.accumulatedConsumptions?.first()?.year)
                assertEquals(
                    1.171,
                    response.responseBody?.accumulatedConsumptions?.first()?.totalConsumption ?: 0.0,
                    0.001
                )
                assertEquals(
                    0.259,
                    response.responseBody?.accumulatedConsumptions?.first()?.totalCost ?: 0.0,
                    0.001
                )
            }
    }
}
package com.bullit.energysimulator.controller

import com.bullit.energysimulator.AbstractIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient

@DirtiesContext
@SpringBootTest
class PowerHandlerTest() : AbstractIntegrationTest() {

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun initClient(context: ApplicationContext) {
        webTestClient = WebTestClient
            .bindToApplicationContext(context)
            .build()
    }

    @Test
    fun `should handle a power csv`(){
        webTestClient
            .get()
            .uri("/import/power")
            .exchange()
            .expectStatus().isOk
            .expectBody(AccumulatedConsumptionDTO::class.java)
            .consumeWith { response ->
                assert(response.responseBody?.accumulatedConsumptions?.size == 2)
                assert(response.responseBody?.accumulatedConsumptions?.first()?.month == 3)
                assert(response.responseBody?.accumulatedConsumptions?.first()?.year == 2024)
                assert(response.responseBody?.accumulatedConsumptions?.first()?.totalConsumption == 1171L)
            }
    }
}
package com.bullit.energysimulator.controller

import com.bullit.energysimulator.CombinedTest
import com.bullit.energysimulator.wiremock.WireMockProxy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@DirtiesContext
@SpringBootTest
class BatteryHandlerTest(
    @Autowired private val proxy: WireMockProxy
) : CombinedTest(proxy) {

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
    fun `should handle a power csv with a battery`() {
        webTestClient
            .get()
            .uri("/import/power?source=battery&heating=boiler")
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
                    0.1728,
                    response.responseBody?.accumulatedConsumptions?.first()?.totalCost ?: 0.0,
                    0.0001
                )
            }
    }

    @Test
    fun `should handle a gas csv with a heat pump`() {
        webTestClient
            .get()
            .uri("/import/gas?source=battery&heating=heatpump")
            .exchange()
            .expectStatus().isOk
            .expectBody(AccumulatedConsumptionDTO::class.java)
            .consumeWith { response ->
                assertEquals(2, response.responseBody?.accumulatedConsumptions?.size)
                assertEquals(3, response.responseBody?.accumulatedConsumptions?.first()?.month)
                assertEquals(2024, response.responseBody?.accumulatedConsumptions?.first()?.year)
                assertEquals(
                    0.007,
                    response.responseBody?.accumulatedConsumptions?.first()?.totalConsumption ?: 0.0,
                    0.001
                )
                assertEquals(
                    0.01,
                    response.responseBody?.accumulatedConsumptions?.first()?.totalCost ?: 0.0,
                    0.01
                )
            }
    }
}
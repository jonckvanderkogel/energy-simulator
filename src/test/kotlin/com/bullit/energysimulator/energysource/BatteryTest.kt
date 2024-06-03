package com.bullit.energysimulator.energysource

import com.bullit.energysimulator.*
import com.bullit.energysimulator.HeatingType.*
import com.bullit.energysimulator.wiremock.WireMockProxy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest(classes = [ContractConfiguration::class, Resilience4jConfiguration::class, WireMockProxy::class])
class BatteryTest(
    @Autowired private val proxy: WireMockProxy,
    @Autowired private val battery: Battery
) : AbstractWiremockTest(proxy) {

    @Test
    fun `should calculate cost against lowest price from the day before`() {
        val result = runBlocking {
            battery.calculateCost(
                PowerConsumption(
                    LocalDateTime.of(2024, 1, 2, 15, 15),
                    10.0,
                    Rate.T1
                ),
                BOILER
            )
        }
        assertTrue(result.isRight())

        // Lowest price from 2024-01-01: -0.000060500000000000
        // Tax: 0.13165
        // Total 0.1315895
        result
            .map {
                assertEquals(1.315895, it.cost, 0.000001)
            }
    }

    @Test
    fun `for gas consumption should delegate to underlying contract`() {
        val result = runBlocking {
            battery.calculateCost(
                GasConsumption(
                    LocalDateTime.of(2024, 1, 1, 11, 15),
                    10.0
                ),
                BOILER
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(10.8097, it.cost, 0.0001)
            }
    }

    @Test
    fun `should calculate cost for heat pump with dynamic power price`() {
        val result = runBlocking {
            battery.calculateCost(
                GasConsumption(
                    LocalDateTime.of(2024, 1, 2, 15, 15),
                    10.0
                ),
                HEATPUMP
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(2.902, it.cost, 0.001)
            }
    }
}
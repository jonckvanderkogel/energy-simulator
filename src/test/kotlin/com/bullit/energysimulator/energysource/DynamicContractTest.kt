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
class DynamicContractTest(
    @Autowired private val proxy: WireMockProxy,
    @Autowired private val dynamicContract: DynamicContract
) : AbstractWiremockTest(proxy) {

    @Test
    fun `should calculate cost with dynamic power price`() {
        val result = runBlocking {
            dynamicContract.calculateCost(
                PowerConsumption(
                    LocalDateTime.of(2024, 1, 1, 11, 15),
                    10.0,
                    Rate.T1
                ),
                BOILER
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(1.3436, it.cost, 0.0001)
            }
    }

    @Test
    fun `should calculate cost with dynamic gas price`() {
        val result = runBlocking {
            dynamicContract.calculateCost(
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
            dynamicContract.calculateCost(
                GasConsumption(
                    LocalDateTime.of(2024, 1, 1, 15, 15),
                    10.0
                ),
                HEATPUMP
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(3.97, it.cost, 0.001)
            }
    }
}
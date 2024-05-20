package com.bullit.energysimulator.contracts

import com.bullit.energysimulator.*
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
    @Autowired private val dynamicContract: EnergyContract<Consumption>
) : AbstractWiremockTest(proxy) {

    @Test
    fun `should calculate cost with dynamic power price`() {
        val result = runBlocking {
            dynamicContract.calculateCost(
                PowerConsumption(
                    LocalDateTime.of(2024, 1, 1, 11, 15),
                    10,
                    Rate.T1
                )
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(0.027104, it, 0.000001)
            }
    }

    @Test
    fun `should calculate cost with dynamic gas price`() {
        val result = runBlocking {
            dynamicContract.calculateCost(
                GasConsumption(
                    LocalDateTime.of(2024, 1, 1, 11, 15),
                    10
                )
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(3.755356, it, 0.000001)
            }
    }
}
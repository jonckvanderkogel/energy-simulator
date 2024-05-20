package com.bullit.energysimulator.contracts

import com.bullit.energysimulator.Resilience4jConfiguration
import com.bullit.energysimulator.wiremock.WireMockProxy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles("dynamic")
@SpringBootTest(classes = [ContractConfiguration::class, Resilience4jConfiguration::class, WireMockProxy::class])
class DynamicContractTest(
    @Autowired private val proxy: WireMockProxy,
    @Autowired private val dynamicContract: EnergyContract
) : AbstractWiremockTest(proxy) {

    @Test
    fun `should give dynamic power price`() {
        val result = runBlocking {
            dynamicContract.powerPrice(LocalDateTime.of(2024, 1, 1, 11, 15))
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(0.0027104, it)
            }
    }

    @Test
    fun `should give dynamic gas price`() {
        val result = runBlocking {
            dynamicContract.gasPrice(LocalDateTime.of(2024, 1, 1, 11, 15))
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(0.3755356, it)
            }
    }
}
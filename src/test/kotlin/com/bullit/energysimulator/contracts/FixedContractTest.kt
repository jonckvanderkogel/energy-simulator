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

@SpringBootTest(classes = [ContractConfiguration::class, Resilience4jConfiguration::class])
class FixedContractTest(
    @Autowired private val fixedContract: EnergyContract
) {

    @Test
    fun `should give t1 price from 7 00 to 21 59`() {
        val result = runBlocking {
            fixedContract.powerPrice(LocalDateTime.of(2024, 1, 1, 11, 15))
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(0.22145, it)
            }
    }

    @Test
    fun `should give t2 price from 22 00 to 6 59`() {
        val result = runBlocking {
            fixedContract.powerPrice(LocalDateTime.of(2024, 1, 1, 5, 45))
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(0.20935, it)
            }
    }

    @Test
    fun `should give gas price`() {
        val result = runBlocking {
            fixedContract.gasPrice(LocalDateTime.of(2024, 1, 1, 5, 45))
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(0.99179, it)
            }
    }
}
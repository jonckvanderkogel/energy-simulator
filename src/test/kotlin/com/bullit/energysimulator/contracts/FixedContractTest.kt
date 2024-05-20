package com.bullit.energysimulator.contracts

import com.bullit.energysimulator.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest(classes = [ContractConfiguration::class, Resilience4jConfiguration::class])
class FixedContractTest(
    @Autowired private val fixedContract: EnergyContract<Consumption>
) {

    @Test
    fun `should use t1 price to calculate cost from 7 00 to 21 59`() {
        val result = runBlocking {
            fixedContract.calculateCost(
                PowerConsumption(
                    LocalDateTime.of(2024, 1, 1, 11, 15),
                    10.0,
                    Rate.T1
                )
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(2.2145, it, 0.0001)
            }
    }

    @Test
    fun `should use t2 price to calculate cost from 22 00 to 6 59`() {
        val result = runBlocking {
            fixedContract.calculateCost(
                PowerConsumption(
                    LocalDateTime.of(2024, 1, 1, 5, 45),
                    10.0,
                    Rate.T1
                )
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(2.0935, it, 0.0001)
            }
    }

    @Test
    fun `should give gas price`() {
        val result = runBlocking {
            fixedContract.calculateCost(
                GasConsumption(
                    LocalDateTime.of(2024, 1, 1, 5, 45),
                    10.0
                )
            )
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(9.9179, it, 0.0001)
            }
    }
}
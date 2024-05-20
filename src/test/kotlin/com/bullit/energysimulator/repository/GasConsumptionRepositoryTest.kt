package com.bullit.energysimulator.repository

import com.bullit.energysimulator.AbstractIntegrationTest
import com.bullit.energysimulator.GasConsumption
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.springframework.boot.test.context.SpringBootTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime

@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@SpringBootTest
class GasConsumptionRepositoryTest(
    @Autowired val gasConsumptionRepository: GasConsumptionRepository
) : AbstractIntegrationTest() {
    @Order(1)
    @Test
    fun `should save GasConsumption`() {
        val gasConsumption = GasConsumption(LocalDateTime.now(), 5.0)
        val gasConsumptionEntity = runBlocking {
            gasConsumptionRepository.saveGasConsumption(gasConsumption)
        }

        assertTrue(gasConsumptionEntity.isRight())
        assertEquals(5.0, gasConsumptionEntity.getOrNull()?.amountConsumed)
    }

    @Order(2)
    @Test
    fun `should fetch GasConsumption`() {
        val gasConsumptionEntity = runBlocking {
            gasConsumptionRepository.findById(1L)
        }

        assertEquals(5.0, gasConsumptionEntity?.amountConsumed)
    }
}
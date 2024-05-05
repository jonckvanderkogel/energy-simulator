package com.bullit.energysimulator.repository

import com.bullit.energysimulator.AbstractIntegrationTest
import com.bullit.energysimulator.GasConsumption
import com.bullit.energysimulator.PowerConsumption
import com.bullit.energysimulator.Rate.T1
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
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
        val gasConsumption = GasConsumption(LocalDateTime.now(), 5L)
        val gasConsumptionEntity = runBlocking {
            gasConsumptionRepository.saveGasConsumption(gasConsumption)
        }

        assertEquals(5L, gasConsumptionEntity?.amountConsumed)
    }

    @Order(2)
    @Test
    fun `should fetch GasConsumption`() {
        val gasConsumptionEntity = runBlocking {
            gasConsumptionRepository.findById(1L)
        }

        assertEquals(5L, gasConsumptionEntity?.amountConsumed)
    }
}
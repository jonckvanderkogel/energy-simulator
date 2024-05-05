package com.bullit.energysimulator.repository

import com.bullit.energysimulator.AbstractIntegrationTest
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
class PowerConsumptionRepositoryTest(
    @Autowired val powerConsumptionRepository: PowerConsumptionRepository
) : AbstractIntegrationTest() {
    @Order(1)
    @Test
    fun `should save PowerConsumption`() {
        val powerConsumption = PowerConsumption(LocalDateTime.now(), 5L, T1)
        val powerConsumptionEntity = runBlocking {
            powerConsumptionRepository.savePowerConsumption(powerConsumption)
        }

        assertEquals(5L, powerConsumptionEntity?.amountConsumed)
    }

    @Order(2)
    @Test
    fun `should fetch PowerConsumption`() {
        val powerConsumptionEntity = runBlocking {
            powerConsumptionRepository.findById(1L)
        }

        assertEquals(5L, powerConsumptionEntity?.amountConsumed)
    }
}
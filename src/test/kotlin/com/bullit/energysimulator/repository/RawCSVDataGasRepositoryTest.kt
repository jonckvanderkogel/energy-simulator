package com.bullit.energysimulator.repository

import com.bullit.energysimulator.AbstractIntegrationTest
import com.bullit.energysimulator.RawCSVDataGas
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime

@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@SpringBootTest
class RawCSVDataGasRepositoryTest(
    @Autowired val rawCSVDataGasRepository: RawCSVDataGasRepository
) : AbstractIntegrationTest() {
    @Order(1)
    @Test
    fun `should save RawCSVDataGas`() {
        val rawCSVDataGas = RawCSVDataGas(LocalDateTime.now(), 5L)
        val rawCSVDataGasEntity = runBlocking {
            rawCSVDataGasRepository.saveRawCSVDataGas(rawCSVDataGas)
        }

        assertEquals(5L, rawCSVDataGasEntity?.meterReading)
    }

    @Order(2)
    @Test
    fun `should fetch RawCSVDataGas`() {
        val rawCSVDataGasEntity = runBlocking {
            rawCSVDataGasRepository.findById(1L)
        }

        assertEquals(5L, rawCSVDataGasEntity?.meterReading)
    }
}
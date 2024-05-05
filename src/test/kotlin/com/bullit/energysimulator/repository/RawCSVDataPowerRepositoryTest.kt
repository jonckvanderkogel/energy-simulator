package com.bullit.energysimulator.repository

import com.bullit.energysimulator.AbstractIntegrationTest
import com.bullit.energysimulator.RawCSVDataPower
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
class RawCSVDataPowerRepositoryTest(
    @Autowired val rawCSVDataPowerRepository: RawCSVDataPowerRepository
) : AbstractIntegrationTest() {
    @Order(1)
    @Test
    fun `should save RawCSVDataPower`() {
        val rawCSVDataPower = RawCSVDataPower(LocalDateTime.now(), 5L, 10L)
        val rawCSVDataPowerEntity = runBlocking {
            rawCSVDataPowerRepository.saveRawCSVDataPower(rawCSVDataPower)
        }

        assertEquals(5L, rawCSVDataPowerEntity?.t1)
        assertEquals(10L, rawCSVDataPowerEntity?.t2)
    }

    @Order(2)
    @Test
    fun `should fetch RawCSVDataPower`() {
        val rawCSVDataPowerEntity = runBlocking {
            rawCSVDataPowerRepository.findById(1L)
        }

        assertEquals(5L, rawCSVDataPowerEntity?.t1)
        assertEquals(10L, rawCSVDataPowerEntity?.t2)
    }
}
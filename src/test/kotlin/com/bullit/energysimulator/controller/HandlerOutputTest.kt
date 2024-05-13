package com.bullit.energysimulator.controller

import com.bullit.energysimulator.GasConsumption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HandlerOutputTest {
    @Test
    fun `should accumulate consumptions`() {
        val handlerOutput = HandlerOutput(errors = listOf(ErrorResponse("wrong", 400)))

        val consumptions = listOf(
            GasConsumption(LocalDateTime.of(2024, 3, 31, 22, 30), 1),
            GasConsumption(LocalDateTime.of(2024, 3, 31, 22, 45), 1),
            GasConsumption(LocalDateTime.of(2024, 3, 31, 23, 0), 1),
            GasConsumption(LocalDateTime.of(2024, 3, 31, 23, 15), 1),
            GasConsumption(LocalDateTime.of(2024, 3, 31, 23, 30), 1),
            GasConsumption(LocalDateTime.of(2024, 3, 31, 23, 45), 1),
            GasConsumption(LocalDateTime.of(2024, 4, 1, 0, 0), 1),
            GasConsumption(LocalDateTime.of(2024, 4, 1, 0, 15), 1),
            GasConsumption(LocalDateTime.of(2024, 4, 1, 0, 30), 1),
            GasConsumption(LocalDateTime.of(2024, 4, 1, 0, 45), 1),
            GasConsumption(LocalDateTime.of(2024, 4, 1, 1, 0), 1)
        )

        val accumulated = consumptions
            .fold(handlerOutput) { acc, consumption ->
                acc.addConsumption(consumption)
            }

        assertEquals(2, accumulated.accumulatedConsumptions.size)
        assertEquals(6L, accumulated.accumulatedConsumptions[0].totalConsumption)
        assertEquals(5L, accumulated.accumulatedConsumptions[1].totalConsumption)
        assertEquals(1, accumulated.errors.size)
    }
}
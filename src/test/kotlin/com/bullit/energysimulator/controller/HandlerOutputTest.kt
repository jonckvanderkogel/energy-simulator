package com.bullit.energysimulator.controller

import com.bullit.energysimulator.ElasticGasConsumptionEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HandlerOutputTest {
    @Test
    fun `should accumulate consumptions`() {
        val consumptionAccumulator = ConsumptionAccumulator(errors = listOf(ErrorResponse("wrong", 400)))

        val consumptions = listOf(
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 3, 31, 22, 30),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 3, 31, 22, 45),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 3, 31, 23, 0),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 3, 31, 23, 15),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 3, 31, 23, 30),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 3, 31, 23, 45),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 4, 1, 0, 0),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 4, 1, 0, 15),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 4, 1, 0, 30),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 4, 1, 0, 45),
                1.0,
                1.0
            ),
            ElasticGasConsumptionEntity(
                LocalDateTime.of(2024, 4, 1, 1, 0),
                1.0,
                1.0
            )
        )

        val accumulated = consumptions
            .fold(consumptionAccumulator) { acc, consumption ->
                acc.addConsumption(consumption)
            }.compact()

        assertEquals(2, accumulated.accumulatedConsumptions.size)
        assertEquals(6.0, accumulated.accumulatedConsumptions[0].totalConsumption)
        assertEquals(6.0, accumulated.accumulatedConsumptions[0].totalCost)
        assertEquals(5.0, accumulated.accumulatedConsumptions[1].totalConsumption)
        assertEquals(5.0, accumulated.accumulatedConsumptions[1].totalCost)
        assertEquals(1, accumulated.errors.size)
    }
}
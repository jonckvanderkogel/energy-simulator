package com.bullit.energysimulator.csv

import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GasFlowTest {
    @Test
    fun `test gas flow`() {
        val inputStream = javaClass.classLoader.getResourceAsStream("gas_test.csv")

        val totalConsumption = runBlocking {
            inputStream
                ?.let {
                    gasFlow(it)
                }
                ?.fold(0.0) { acc, consumption ->
                    acc + consumption.amountConsumed
                }
        } ?: 0.0

        assertEquals(0.007, totalConsumption, 0.001)
    }
}
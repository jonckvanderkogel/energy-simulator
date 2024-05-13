package com.bullit.energysimulator.csv

import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PowerFlowTest {
    @Test
    fun `test power flow`() {
        val inputStream = javaClass.classLoader.getResourceAsStream("power_test.csv")

        val totalConsumption = runBlocking {
            inputStream
                ?.let {
                    powerFlow(it)
                }
                ?.fold(0L) { acc, consumption ->
                    acc + consumption.amountConsumed
                }
        } ?: 0

        assertEquals(362L, totalConsumption)
    }
}
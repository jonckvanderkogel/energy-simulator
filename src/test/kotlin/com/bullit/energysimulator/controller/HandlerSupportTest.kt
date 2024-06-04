package com.bullit.energysimulator.controller

import com.bullit.energysimulator.*
import com.bullit.energysimulator.EnergySourceType.*
import com.bullit.energysimulator.HeatingType.BOILER
import com.bullit.energysimulator.HeatingType.HEATPUMP
import com.bullit.energysimulator.PowerConsumptionType.*
import com.bullit.energysimulator.Rate.T2
import com.bullit.energysimulator.energysource.ContractConfiguration.SCOP
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class HandlerSupportTest {
    @Test
    fun `should generate power consumption with power consumptiont type of general when heating type is not heating`() {
        val esEntity = generateEsEntity(
            HeatingType.NOT_HEATING,
            DYNAMIC,
            PowerConsumption(
                LocalDateTime.of(2024, 1, 2, 15, 15),
                10.0,
                Rate.T1
            ),
            1.0
        )

        assertTrue(esEntity is ElasticPowerConsumptionEntity)
        assertTrue((esEntity as ElasticPowerConsumptionEntity).powerConsumptionType == GENERAL)
        assertTrue(esEntity.energySourceType == DYNAMIC)
    }

    /**
     * Actually the combination of power consumption with heating type boiler is non-sensical
     */
    @Test
    fun `should throw when we have a power consumption with heating type of boiler`() {
        assertThrows<IllegalStateException> {
            generateEsEntity(
                HeatingType.BOILER,
                DYNAMIC,
                PowerConsumption(
                    LocalDateTime.of(2024, 1, 2, 15, 15),
                    10.0,
                    Rate.T1
                ),
                1.0
            )
        }
    }

    @Test
    fun `should transform to PowerConsumption when consumption is gas and heating type is heatpump`() {
        val consumption = transformConsumption(
            GasConsumption(
                LocalDateTime.of(2024, 1, 2, 15, 15),
                10.0
            ),
            HEATPUMP,
            SCOP(4.0)
        )

        assertTrue(consumption is PowerConsumption)
        assertTrue((consumption as PowerConsumption).rate == T2)
        assertEquals(22.05, consumption.amountConsumed)
    }

    @Test
    fun `should keep as GasConsumption when consumption is gas and heating type is boiler`() {
        val consumption = transformConsumption(
            GasConsumption(
                LocalDateTime.of(2024, 1, 2, 15, 15),
                10.0
            ),
            BOILER,
            SCOP(4.0)
        )

        assertTrue(consumption is GasConsumption)
        assertEquals(10.0, consumption.amountConsumed)
    }
}
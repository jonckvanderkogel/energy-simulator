package com.bullit.energysimulator.energysource

import arrow.core.Either
import com.bullit.energysimulator.*
import com.bullit.energysimulator.energysource.ContractConfiguration.SCOP
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import java.time.LocalDateTime

interface EnergySource {
    suspend fun calculateCost(
        consumption: Consumption,
        heatingType: HeatingType,
        customPriceDateTime: LocalDateTime = consumption.dateTime
    ): Either<ApplicationErrors, EsEntity>

    fun Double.transformGasAmountForHeatPump(scop: SCOP): Double =
        this * 8.82 / scop.scopValue

    fun GasConsumption.calculateRate(): Rate =
        when {
            this.dateTime.hour in 7..22 -> Rate.T2
            else -> Rate.T1
        }
}
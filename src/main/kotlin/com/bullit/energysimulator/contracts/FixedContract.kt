package com.bullit.energysimulator.contracts

import arrow.core.Either
import arrow.core.right
import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.GasConsumption
import com.bullit.energysimulator.PowerConsumption
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import java.time.LocalDateTime

class FixedContract(
    private val powerPriceT1: Double,
    private val powerPriceT2: Double,
    private val gasPrice: Double,
) : EnergyContract<Consumption> {
    private fun powerPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double> =
        when {
            dateTime.hour in 22..23 || dateTime.hour in 0..6 -> powerPriceT2.right()
            else -> powerPriceT1.right()
        }

    private fun gasPrice(): Either<ApplicationErrors, Double> = gasPrice.right()

    override suspend fun calculateCost(consumption: Consumption): Either<ApplicationErrors, Double> =
        when (consumption) {
            is PowerConsumption -> powerPrice(consumption.dateTime).map { it  * consumption.amountConsumed }
            is GasConsumption -> gasPrice().map { it * consumption.amountConsumed}
        }
}
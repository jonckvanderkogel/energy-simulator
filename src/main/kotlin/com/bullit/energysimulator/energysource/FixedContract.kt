package com.bullit.energysimulator.energysource

import arrow.core.Either
import arrow.core.right
import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.GasConsumption
import com.bullit.energysimulator.PowerConsumption
import com.bullit.energysimulator.Rate
import com.bullit.energysimulator.errorhandling.ApplicationErrors

class FixedContract(
    private val powerPriceT1: Double,
    private val powerPriceT2: Double,
    private val gasPrice: Double,
) : EnergySource {
    private fun powerPrice(rate: Rate): Either<ApplicationErrors, Double> =
        when (rate) {
            Rate.T1 -> powerPriceT1.right()
            Rate.T2 -> powerPriceT2.right()
        }

    private fun gasPrice(): Either<ApplicationErrors, Double> = gasPrice.right()

    override suspend fun calculateCost(consumption: Consumption): Either<ApplicationErrors, Double> =
        when (consumption) {
            is PowerConsumption -> powerPrice(consumption.rate).map { it  * consumption.amountConsumed }
            is GasConsumption -> gasPrice().map { it * consumption.amountConsumed}
        }
}
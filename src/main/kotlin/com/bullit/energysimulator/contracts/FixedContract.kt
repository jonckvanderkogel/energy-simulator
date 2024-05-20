package com.bullit.energysimulator.contracts

import arrow.core.Either
import arrow.core.right
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import java.time.LocalDateTime

class FixedContract(
    private val powerPriceT1: Double,
    private val powerPriceT2: Double,
    private val gasPrice: Double,
) : EnergyContract {
    override suspend fun powerPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double> =
        when {
            dateTime.hour in 22..23 || dateTime.hour in 0..6 -> powerPriceT2.right()
            else -> powerPriceT1.right()
        }

    override suspend fun gasPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double> = gasPrice.right()
}
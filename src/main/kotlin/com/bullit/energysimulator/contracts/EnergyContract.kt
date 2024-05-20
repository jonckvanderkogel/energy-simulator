package com.bullit.energysimulator.contracts

import arrow.core.Either
import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.errorhandling.ApplicationErrors

interface EnergyContract<T: Consumption> {
    suspend fun calculateCost(consumption: T): Either<ApplicationErrors, Double>
}
package com.bullit.energysimulator.energysource

import arrow.core.Either
import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.errorhandling.ApplicationErrors

interface EnergySource {
    suspend fun calculateCost(consumption: Consumption): Either<ApplicationErrors, Double>
}
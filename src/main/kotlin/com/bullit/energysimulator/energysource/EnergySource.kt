package com.bullit.energysimulator.energysource

import arrow.core.Either
import com.bullit.energysimulator.*
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import java.time.LocalDateTime

interface EnergySource {
    suspend fun calculateCost(
        consumption: Consumption,
        customPriceDateTime: LocalDateTime = consumption.dateTime
    ): Either<ApplicationErrors, Double>
}
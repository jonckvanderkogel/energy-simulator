package com.bullit.energysimulator.contracts

import arrow.core.Either
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import java.time.LocalDateTime

interface EnergyContract {
    suspend fun powerPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double>
    suspend fun gasPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double>
}
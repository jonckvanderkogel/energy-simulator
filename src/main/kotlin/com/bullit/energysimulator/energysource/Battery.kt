package com.bullit.energysimulator.energysource

import arrow.core.Either
import arrow.core.flatMap
import com.bullit.energysimulator.*
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.CouldNotCalculateMinimumPriceError
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import kotlinx.coroutines.future.await
import java.time.LocalDate
import java.time.LocalDateTime

class Battery(
    private val powerTariffCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>>,
    private val underlyingContract: EnergySource
) : EnergySource {
    /*
    We are making the assumption here that the battery will be giving you energy against the lowest price from
    the day before the one you are consuming the energy.
     */
    override suspend fun calculateCost(
        consumption: Consumption,
        customPriceDateTime: LocalDateTime
    ): Either<ApplicationErrors, Double> =
        when (consumption) {
            is PowerConsumption -> powerTariffCache
                .lowestPriceDayBefore(consumption.dateTime)
                .flatMap {
                    underlyingContract.calculateCost(
                        consumption,
                        it
                    )
                }

            is GasConsumption -> underlyingContract.calculateCost(consumption)
        }

    private suspend fun AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>>.lowestPriceDayBefore(
        dateTime: LocalDateTime
    ): Either<ApplicationErrors, LocalDateTime> = get(dateTime.toLocalDate().minusDays(1L))
        .await()
        .flatMap {
            it.lowestPrice().toEither { CouldNotCalculateMinimumPriceError(dateTime) }
        }
        .map {
            it.dateTime
        }

    private fun List<EnergyTariff>.lowestPrice() =
        minWithOrNull(
            compareBy { it.rateUsage }
        )
}
package com.bullit.energysimulator.contracts

import arrow.core.Either
import arrow.core.raise.either
import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.GasConsumption
import com.bullit.energysimulator.PowerConsumption
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.MissingTariffError
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class DynamicContract(
    private val easyEnergyClient: EasyEnergyClient
) : EnergyContract<Consumption> {
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun buildCache(
        easyEnergyClientFun: suspend (date: LocalDate) -> Either<ApplicationErrors, List<EnergyTariff>>
    ): AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>> =
        Caffeine.newBuilder()
            .buildAsync { key, _ ->
                scope.async {
                    easyEnergyClientFun(key)
                }.asCompletableFuture()
            }

    private val powerTariffCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>> =
        buildCache(easyEnergyClient::fetchPowerPrices)

    private val gasTariffCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>> =
        buildCache(easyEnergyClient::fetchGasPrices)

    private suspend fun energyPrice(
        cacheFun: (date: LocalDate) -> CompletableFuture<Either<ApplicationErrors, List<EnergyTariff>>>,
        dateTime: LocalDateTime
    ): Either<ApplicationErrors, Double> =
        either {
            val tariffs = cacheFun(dateTime.toLocalDate())
                .await()
                .bind()

            tariffs
                .findByLocalDateTime(dateTime)
                .bind()
                .rateUsage
        }

    private suspend fun powerPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double> =
        energyPrice(
            powerTariffCache::get,
            dateTime
        )

    private suspend fun gasPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double> =
        energyPrice(
            gasTariffCache::get,
            dateTime
        )

    private fun List<EnergyTariff>.findByLocalDateTime(findDateTime: LocalDateTime) =
        find { it.dateTime.year == findDateTime.year
                && it.dateTime.month == findDateTime.month
                && it.dateTime.monthValue == findDateTime.monthValue
                && it.dateTime.hour == findDateTime.hour}
            .toEither { MissingTariffError(findDateTime) }

    override suspend fun calculateCost(consumption: Consumption): Either<ApplicationErrors, Double> =
        when (consumption) {
            is PowerConsumption -> powerPrice(consumption.dateTime).map { it  * consumption.amountConsumed }
            is GasConsumption -> gasPrice(consumption.dateTime).map { it * consumption.amountConsumed}
        }
}
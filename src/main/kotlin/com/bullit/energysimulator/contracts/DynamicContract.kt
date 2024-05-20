package com.bullit.energysimulator.contracts

import arrow.core.Either
import arrow.core.raise.either
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

class DynamicContract(
    private val easyEnergyClient: EasyEnergyClient
) : EnergyContract {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val entityCountCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>> =
        Caffeine.newBuilder()
            .buildAsync { key, _ ->
                scope.async {
                    easyEnergyClient.fetchEnergyPrices(key)
                }.asCompletableFuture()
            }

    override suspend fun powerPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double> =
        either {
            val tariffs = entityCountCache
                .get(dateTime.toLocalDate())
                .await()
                .bind()

            tariffs
                .findByLocalDateTime(dateTime)
                .bind()
                .rateUsage
        }

    private fun List<EnergyTariff>.findByLocalDateTime(findDateTime: LocalDateTime) =
        find { it.dateTime.year == findDateTime.year
                && it.dateTime.month == findDateTime.month
                && it.dateTime.monthValue == findDateTime.monthValue
                && it.dateTime.hour == findDateTime.hour}
            .toEither { MissingTariffError(findDateTime) }

    override suspend fun gasPrice(dateTime: LocalDateTime): Either<ApplicationErrors, Double> {
        TODO("Not yet implemented")
    }
}
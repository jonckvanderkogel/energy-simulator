package com.bullit.energysimulator.energysource

import arrow.core.Either
import arrow.core.raise.either
import com.bullit.energysimulator.*
import com.bullit.energysimulator.EnergySourceType.DYNAMIC
import com.bullit.energysimulator.HeatingType.BOILER
import com.bullit.energysimulator.HeatingType.HEATPUMP
import com.bullit.energysimulator.energysource.ContractConfiguration.*
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.MissingTariffError
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import kotlinx.coroutines.future.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class DynamicContract(
    private val taxPower: Double,
    private val taxGas: Double,
    private val scop: SCOP,
    private val powerTariffCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>>,
    private val gasTariffCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>>
) : EnergySource {

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
        find {
            it.dateTime.year == findDateTime.year
                    && it.dateTime.month == findDateTime.month
                    && it.dateTime.monthValue == findDateTime.monthValue
                    && it.dateTime.hour == findDateTime.hour
        }
            .toEither { MissingTariffError(findDateTime) }

    override suspend fun calculateCost(
        consumption: Consumption,
        heatingType: HeatingType,
        customPriceDateTime: LocalDateTime
    ): Either<ApplicationErrors, EsEntity> =
        when (consumption) {
            is PowerConsumption -> powerPrice(customPriceDateTime)
                .map {
                    val cost = (it + taxPower) * consumption.amountConsumed
                    ElasticPowerConsumptionEntity(
                        consumption.dateTime,
                        consumption.amountConsumed,
                        consumption.rate,
                        cost,
                        DYNAMIC,
                        PowerConsumptionType.GENERAL
                    )
                }

            is GasConsumption ->
                when (heatingType) {
                    BOILER -> gasPrice(customPriceDateTime)
                        .map {
                            val cost = (it + taxGas) * consumption.amountConsumed
                            ElasticGasConsumptionEntity(
                                consumption.dateTime,
                                consumption.amountConsumed,
                                cost,
                                DYNAMIC
                            )
                        }

                    HEATPUMP -> powerPrice(customPriceDateTime)
                        .map {
                            val amountConsumed = consumption.amountConsumed.transformGasAmountForHeatPump(scop)
                            val cost = (it + taxPower) * amountConsumed
                            ElasticPowerConsumptionEntity(
                                consumption.dateTime,
                                amountConsumed,
                                consumption.calculateRate(),
                                cost,
                                DYNAMIC,
                                PowerConsumptionType.HEATING
                            )
                        }
                }
        }
}
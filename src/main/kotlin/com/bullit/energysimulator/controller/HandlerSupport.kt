package com.bullit.energysimulator.controller

import arrow.core.Either
import arrow.core.raise.either
import com.bullit.energysimulator.*
import com.bullit.energysimulator.HeatingType.*
import com.bullit.energysimulator.PowerConsumptionType.*
import com.bullit.energysimulator.energysource.ContractConfiguration.SCOP
import com.bullit.energysimulator.energysource.EnergySource
import com.bullit.energysimulator.energysource.EnergySourceProvider
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.MissingParameterError
import org.springframework.web.reactive.function.server.ServerRequest
import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val status: Int
)

data class ConsumptionAccumulator(
    val accumulatedConsumptions: List<AccumulatedConsumption> = emptyList(),
    val errors: List<ErrorResponse> = emptyList(),
    val consumptionsInSamePeriod: List<EsEntity> = emptyList()
) {
    fun addError(errorResponse: ErrorResponse): ConsumptionAccumulator =
        ConsumptionAccumulator(this.accumulatedConsumptions, this.errors + errorResponse, this.consumptionsInSamePeriod)

    fun addConsumption(consumption: EsEntity): ConsumptionAccumulator =
        if (consumptionsInSamePeriod.lastMonthYearSame(consumption)) {
            ConsumptionAccumulator(
                this.accumulatedConsumptions,
                this.errors,
                this.consumptionsInSamePeriod + consumption
            )
        } else {
            ConsumptionAccumulator(
                this.accumulatedConsumptions.plusMaybe(consumptionsInSamePeriod.toAccumulatedConsumption()),
                this.errors,
                listOf(consumption)
            )
        }

    fun compact(): ConsumptionAccumulator =
        ConsumptionAccumulator(
            this.accumulatedConsumptions.plusMaybe(consumptionsInSamePeriod.toAccumulatedConsumption()),
            this.errors
        )
}

fun <T> List<T>.plusMaybe(item: T?): List<T> {
    return if (item == null) {
        this
    } else {
        this.plus(item)
    }
}

internal fun ConsumptionAccumulator.toAccumulatedConsumptionDTO(): AccumulatedConsumptionDTO =
    AccumulatedConsumptionDTO(
        accumulatedConsumptions,
        errors
    )

data class AccumulatedConsumptionDTO(
    val accumulatedConsumptions: List<AccumulatedConsumption>,
    val errors: List<ErrorResponse>
)

private fun List<EsEntity>.lastMonthYearSame(consumption: EsEntity): Boolean =
    if (isNotEmpty()) {
        val last = last()
        last.dateTime.sameMonthYear(consumption.dateTime)
    } else {
        true
    }

private fun List<EsEntity>.totalConsumption(): Double =
    fold(0.0) { acc, consumption ->
        acc + consumption.amountConsumed
    }

private fun List<EsEntity>.totalCost(): Double =
    fold(0.0) { acc, consumption ->
        acc + consumption.cost
    }

private fun List<EsEntity>.lastMonthYear(): Pair<Int, Int> =
    last().dateTime.monthValue to last().dateTime.year

private fun List<EsEntity>.toAccumulatedConsumption(): AccumulatedConsumption? =
    if (isEmpty()) {
        null
    } else {
        lastMonthYear()
            .let { (month, year) ->
                AccumulatedConsumption(
                    month,
                    year,
                    totalConsumption(),
                    totalCost()
                )
            }
    }

private fun LocalDateTime.sameMonthYear(other: LocalDateTime): Boolean =
    this.year == other.year && this.month == other.month

data class AccumulatedConsumption(
    val month: Int, // from 1 to 12 for each month
    val year: Int,
    val totalConsumption: Double,
    val totalCost: Double
)

internal fun parseParameters(
    request: ServerRequest,
    energySourceProvider: EnergySourceProvider
): Either<ApplicationErrors, Triple<HeatingType, EnergySourceType, EnergySource>> = either {
    val energySourceType = parseEnergySourceParam(request).bind()

    val heatingType = parseHeatingParam(request).bind()

    Triple(heatingType, energySourceType, energySourceProvider(energySourceType))
}

private fun parseEnergySourceParam(
    request: ServerRequest
): Either<ApplicationErrors, EnergySourceType> =
    parseRequestParam(request, "source", EnergySourceType.Companion::parse)


private fun parseHeatingParam(
    request: ServerRequest
): Either<ApplicationErrors, HeatingType> =
    either {
        if (request.path().contains("gas")) {
            parseRequestParam(request, "heating", Companion::parse).bind()
        } else {
            NOT_HEATING
        }
    }

private inline fun <reified T> parseRequestParam(
    request: ServerRequest,
    paramName: String,
    parseFunction: (String, String) -> Either<ApplicationErrors, T>
): Either<ApplicationErrors, T> where T : Enum<T>, T : ParsableEnum<T> = either {
    val paramValue = request
        .queryParam(paramName)
        .toEither { MissingParameterError(paramName) }.bind()

    parseFunction(paramValue, paramName).bind()
}

fun transformConsumption(consumption: Consumption, heatingType: HeatingType, scop: SCOP): Consumption =
    if (consumption is GasConsumption && heatingType == HEATPUMP) {
        PowerConsumption(
            consumption.dateTime,
            consumption.amountConsumed.transformGasAmountForHeatPump(scop),
            consumption.calculateRate()
        )
    } else {
        consumption
    }

private fun Double.transformGasAmountForHeatPump(scop: SCOP): Double =
    this * 8.82 / scop.scopValue

private fun GasConsumption.calculateRate(): Rate =
    when {
        this.dateTime.hour in 7..22 -> Rate.T2
        else -> Rate.T1
    }

fun generateEsEntity(
    heatingType: HeatingType,
    energySourceType: EnergySourceType,
    consumption: Consumption,
    cost: Double
): EsEntity =
    when (consumption) {
        is PowerConsumption -> consumption.toElasticPowerConsumption(cost, energySourceType, heatingType.toPowerConsumptionType())
        is GasConsumption -> consumption.toElasticGasConsumption(cost, energySourceType)
    }

private fun HeatingType.toPowerConsumptionType(): PowerConsumptionType =
    when (this) {
        NOT_HEATING -> GENERAL
        HEATPUMP -> HEATING
        BOILER -> throw IllegalStateException("we should never have boiler as heating type for a power consumption")
    }

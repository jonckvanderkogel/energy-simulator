package com.bullit.energysimulator.controller

import arrow.core.Either
import arrow.core.raise.either
import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.EsEntity
import com.bullit.energysimulator.contracts.EnergyContract
import com.bullit.energysimulator.contracts.EnergyContractProvider
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.MissingArgumentError
import com.bullit.energysimulator.toEither
import org.springframework.web.reactive.function.server.ServerRequest
import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val status: Int,
    val error: String = "Bad Request",
    val timestamp: Long = System.currentTimeMillis()
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

internal fun parseContractType(
    request: ServerRequest,
    energyContractProvider: EnergyContractProvider<Consumption>
): Either<ApplicationErrors, Pair<ContractType, EnergyContract<Consumption>>> = either {
    val contractTypeParameter = request
        .queryParam("contract")
        .toEither { MissingArgumentError("contract") }.bind()

    val contractType = ContractType
        .parseContractTypeString(contractTypeParameter)
        .bind()

    contractType to energyContractProvider(contractType)
}

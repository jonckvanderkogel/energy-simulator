package com.bullit.energysimulator.controller

import arrow.core.Either
import arrow.core.raise.either
import com.bullit.energysimulator.*
import com.bullit.energysimulator.contracts.EnergyContractProvider
import com.bullit.energysimulator.csv.gasFlow
import com.bullit.energysimulator.csv.powerFlow
import com.bullit.energysimulator.elasticsearch.ElasticsearchService
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.MissingArgumentError
import com.bullit.energysimulator.errorhandling.joinMessages
import com.bullit.energysimulator.repository.GasConsumptionRepository
import com.bullit.energysimulator.repository.PowerConsumptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

@Configuration
class HandlerConfiguration {
    @Bean
    fun powerHandler(
        powerConsumptionRepository: PowerConsumptionRepository,
        elasticsearchService: ElasticsearchService,
        energyContractProvider: EnergyContractProvider<Consumption>,
        @Value("\${files.power}") powerCsvName: String
    ): PowerHandler {
        return PowerHandler(
            powerCsvName,
            ::powerFlow,
            energyContractProvider,
            PowerConsumption::toElasticPowerConsumption,
            elasticsearchService::saveConsumption
        )
    }

    @Bean
    fun gasHandler(
        gasConsumptionRepository: GasConsumptionRepository,
        elasticsearchService: ElasticsearchService,
        energyContractProvider: EnergyContractProvider<Consumption>,
        @Value("\${files.gas}") gasCsvName: String
    ): GasHandler {
        return GasHandler(
            gasCsvName,
            ::gasFlow,
            energyContractProvider,
            GasConsumption::toElasticGasConsumption,
            elasticsearchService::saveConsumption
        )
    }

    @Bean
    fun searchPower(
        elasticsearchService: ElasticsearchService
    ): SearchHandler<ElasticPowerConsumptionEntity> = SearchHandler { request ->
        either {
            val gte = request.queryParam("gte")
                .map { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
                .toEither { MissingArgumentError("gte") }.bind()

            val lte = request.queryParam("lte")
                .map { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
                .toEither { MissingArgumentError("lte") }.bind()

            gte to lte
        }
            .map { (gte, lte) ->
                elasticsearchService.searchByDateRange<ElasticPowerConsumptionEntity>(
                    gte,
                    lte,
                )
            }
            .fold(
                ifLeft = { left ->
                    ServerResponse
                        .badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(left.joinMessages())
                },
                ifRight = {
                    ServerResponse.ok().bodyAndAwait(
                        it
                    )
                }
            )
    }
}

fun interface SearchHandler<S : EsEntity> {
    suspend fun search(request: ServerRequest): ServerResponse
}

abstract class RouteHandler<T : Consumption, S : EsEntity>(
    private val csvName: String,
    private val flow: suspend (InputStream) -> Flow<T>,
    private val energyContractProvider: EnergyContractProvider<Consumption>,
    private val transformFun: T.(Double, ContractType) -> S,
    private val esSave: suspend (S) -> Either<ApplicationErrors, S>
) {
    suspend fun handleFlow(request: ServerRequest): ServerResponse = either {
        val contractTypeParameter = request
            .queryParam("contract")
            .getOrNull() ?: "Contract parameter"

        val contractType = ContractType
            .parseContractTypeString(contractTypeParameter)
            .bind()

        contractType to energyContractProvider(contractType)
    }
        .fold(
            ifLeft = { left ->
                ServerResponse
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValueAndAwait(left.joinMessages())
            },
            ifRight = { (contractType, energyContract) ->
                ServerResponse.ok().bodyValueAndAwait(
                    flow( streamCsv().invoke(csvName))
                        .map {
                            either {
                                val cost = energyContract.calculateCost(it).bind()
                                esSave(it.transformFun(cost, contractType)).bind()
                            }

                        }
                        .fold(ConsumptionAccumulator()) { acc, either ->
                            either.fold(
                                ifLeft = { errors ->
                                    acc.addError(ErrorResponse(errors.joinMessages(), HttpStatus.BAD_REQUEST.value()))
                                },
                                ifRight = {
                                    acc.addConsumption(it)
                                }
                            )
                        }
                        .compact()
                        .toAccumulatedConsumptionDTO()
                )
            }
        )

    private fun streamCsv(): (String) -> InputStream = { csvName ->
        javaClass.classLoader.getResourceAsStream(csvName)!!
    }
}

class PowerHandler(
    powerCsvName: String,
    flow: suspend (InputStream) -> Flow<PowerConsumption>,
    energyContractProvider: EnergyContractProvider<Consumption>,
    transformFun: (PowerConsumption, Double, ContractType) -> ElasticPowerConsumptionEntity,
    esSave: suspend (ElasticPowerConsumptionEntity) -> Either<ApplicationErrors, ElasticPowerConsumptionEntity>
) : RouteHandler<PowerConsumption, ElasticPowerConsumptionEntity>(
    powerCsvName, flow, energyContractProvider, transformFun, esSave
)

class GasHandler(
    gasCsvName: String,
    flow: suspend (InputStream) -> Flow<GasConsumption>,
    energyContractProvider: EnergyContractProvider<Consumption>,
    transformFun: (GasConsumption, Double, ContractType) -> ElasticGasConsumptionEntity,
    esSave: suspend (ElasticGasConsumptionEntity) -> Either<ApplicationErrors, ElasticGasConsumptionEntity>
) : RouteHandler<GasConsumption, ElasticGasConsumptionEntity>(
    gasCsvName, flow, energyContractProvider, transformFun, esSave
)

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

private fun ConsumptionAccumulator.toAccumulatedConsumptionDTO(): AccumulatedConsumptionDTO =
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
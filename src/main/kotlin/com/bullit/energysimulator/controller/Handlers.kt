package com.bullit.energysimulator.controller

import arrow.core.Either
import arrow.core.raise.either
import com.bullit.energysimulator.*
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

@Configuration
class HandlerConfiguration {
    @Bean
    fun powerHandler(
        powerConsumptionRepository: PowerConsumptionRepository,
        elasticsearchService: ElasticsearchService
    ): PowerHandler {
        val inputStream = javaClass.classLoader.getResourceAsStream("P1e-2024-1-01-2024-5-01.csv")!!
        return PowerHandler(
            inputStream,
            ::powerFlow,
            powerConsumptionRepository::savePowerConsumption,
            PowerConsumptionEntity::toEs,
            elasticsearchService::saveConsumption
        )
    }

    @Bean
    fun gasHandler(
        gasConsumptionRepository: GasConsumptionRepository,
        elasticsearchService: ElasticsearchService
    ): GasHandler {
        val inputStream = javaClass.classLoader.getResourceAsStream("P1g-2024-1-01-2024-5-01.csv")!!
        return GasHandler(
            inputStream,
            ::gasFlow,
            gasConsumptionRepository::saveGasConsumption,
            GasConsumptionEntity::toEs,
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

fun interface SearchHandler<T : EsEntity> {
    suspend fun search(request: ServerRequest): ServerResponse
}

abstract class RouteHandler<T : Consumption, R : DbEntity, S : EsEntity>(
    private val inputStream: InputStream,
    private val flow: suspend (InputStream) -> Flow<T>,
    private val dbSave: suspend (T) -> Either<ApplicationErrors, R>,
    private val transformFun: R.() -> S,
    private val esSave: suspend (S) -> Either<ApplicationErrors, S>
) {
    /*
    The Kotlin compiler wrongly shows that the request arg is not needed here.
    Spring definitely requires it.
     */
    suspend fun handleFlow(request: ServerRequest): ServerResponse =
        ServerResponse.ok().bodyValueAndAwait(
            flow(inputStream)
                .map {
                    either {
                        val dbEntity = dbSave(it).bind()
                        val esEntity = esSave(dbEntity.transformFun()).bind()
                        esEntity
                    }
                }
                .fold(ConsumptionAccumulator()) { acc, either ->
                    either.fold(
                        ifLeft = { errors ->
                            acc.addError(ErrorResponse(errors.joinMessages(), HttpStatus.BAD_REQUEST.value()))
                        },
                        ifRight = { esConsumption ->
                            val consumption = esConsumption.toDomain()
                            acc.addConsumption(consumption)
                        }
                    )
                }
                .compact()
                .toAccumulatedConsumptionDTO()
        )
}

class PowerHandler(
    inputStream: InputStream,
    flow: suspend (InputStream) -> Flow<PowerConsumption>,
    dbSave: suspend (PowerConsumption) -> Either<ApplicationErrors, PowerConsumptionEntity>,
    transformFun: (PowerConsumptionEntity) -> ElasticPowerConsumptionEntity,
    esSave: suspend (ElasticPowerConsumptionEntity) -> Either<ApplicationErrors, ElasticPowerConsumptionEntity>
) : RouteHandler<PowerConsumption, PowerConsumptionEntity, ElasticPowerConsumptionEntity>(
    inputStream, flow, dbSave, transformFun, esSave
)

class GasHandler(
    inputStream: InputStream,
    flow: suspend (InputStream) -> Flow<GasConsumption>,
    save: suspend (GasConsumption) -> Either<ApplicationErrors, GasConsumptionEntity>,
    transformFun: (GasConsumptionEntity) -> ElasticGasConsumptionEntity,
    esSave: suspend (ElasticGasConsumptionEntity) -> Either<ApplicationErrors, ElasticGasConsumptionEntity>
) : RouteHandler<GasConsumption, GasConsumptionEntity, ElasticGasConsumptionEntity>(
    inputStream, flow, save, transformFun, esSave
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
    val consumptionsInSamePeriod: List<Consumption> = emptyList()
) {
    fun addError(errorResponse: ErrorResponse): ConsumptionAccumulator =
        ConsumptionAccumulator(this.accumulatedConsumptions, this.errors + errorResponse, this.consumptionsInSamePeriod)

    fun addConsumption(consumption: Consumption): ConsumptionAccumulator =
        if (consumptionsInSamePeriod.lastMonthYearSame(consumption)) {
            ConsumptionAccumulator(
                this.accumulatedConsumptions,
                this.errors,
                this.consumptionsInSamePeriod + consumption
            )
        } else {
            ConsumptionAccumulator(
                this.accumulatedConsumptions + consumptionsInSamePeriod.toAccumulatedConsumption(),
                this.errors,
                listOf(consumption)
            )
        }

    fun compact(): ConsumptionAccumulator =
        ConsumptionAccumulator(
            this.accumulatedConsumptions + consumptionsInSamePeriod.toAccumulatedConsumption(),
            this.errors
        )

}

private fun ConsumptionAccumulator.toAccumulatedConsumptionDTO() =
    AccumulatedConsumptionDTO(
        accumulatedConsumptions,
        errors
    )

data class AccumulatedConsumptionDTO(
    val accumulatedConsumptions: List<AccumulatedConsumption>,
    val errors: List<ErrorResponse>
)

private fun List<Consumption>.lastMonthYearSame(consumption: Consumption): Boolean =
    if (isNotEmpty()) {
        val last = last()
        last.dateTime.sameMonthYear(consumption.dateTime)
    } else {
        true
    }

private fun List<Consumption>.total(): Long =
    fold(0L) { acc, consumption ->
        acc + consumption.amountConsumed
    }

private fun List<Consumption>.lastMonthYear(): Pair<Int, Int> =
    last().dateTime.monthValue to last().dateTime.year

private fun List<Consumption>.toAccumulatedConsumption(): AccumulatedConsumption =
    lastMonthYear()
        .let { (month, year) ->
            AccumulatedConsumption(
                month,
                year,
                total()
            )
        }

private fun LocalDateTime.sameMonthYear(other: LocalDateTime): Boolean =
    this.year == other.year && this.month == other.month

data class AccumulatedConsumption(
    val month: Int, // from 1 to 12 for each month
    val year: Int,
    val totalConsumption: Long
)
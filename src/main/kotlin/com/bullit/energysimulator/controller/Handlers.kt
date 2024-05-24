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

@Configuration
class HandlerConfiguration {
    @Bean
    fun powerHandler(
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
    suspend fun handleFlow(request: ServerRequest): ServerResponse =
        parseContractType(request, energyContractProvider)
            .fold(
                ifLeft = { left ->
                    ServerResponse
                        .badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(left.joinMessages())
                },
                ifRight = { (contractType, energyContract) ->
                    ServerResponse.ok().bodyValueAndAwait(
                        flow(streamCsv().invoke(csvName))
                            .map {
                                either {
                                    val cost = energyContract.calculateCost(it).bind()
                                    esSave(it.transformFun(cost, contractType)).bind()
                                }

                            }
                            .fold(ConsumptionAccumulator()) { acc, either ->
                                either.fold(
                                    ifLeft = { errors ->
                                        acc.addError(
                                            ErrorResponse(
                                                errors.joinMessages(),
                                                HttpStatus.BAD_REQUEST.value()
                                            )
                                        )
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

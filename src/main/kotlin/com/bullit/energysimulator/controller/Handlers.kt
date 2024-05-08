package com.bullit.energysimulator.controller

import arrow.core.Either
import arrow.core.flatMap
import com.bullit.energysimulator.*
import com.bullit.energysimulator.csv.gasFlow
import com.bullit.energysimulator.csv.powerFlow
import com.bullit.energysimulator.elasticsearch.ElasticsearchService
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.joinMessages
import com.bullit.energysimulator.repository.GasConsumptionRepository
import com.bullit.energysimulator.repository.PowerConsumptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import java.io.InputStream

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
            elasticsearchService::savePowerConsumption
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
            elasticsearchService::saveGasConsumption
        )
    }
}

abstract class RouteHandler<T : Consumption, R : DbEntity, S : EsEntity>(
    private val inputStream: InputStream,
    private val flow: suspend (InputStream) -> Flow<T>,
    private val dbSave: suspend (T) -> Either<ApplicationErrors, R>,
    private val transformFun: R.() -> S,
    private val esSave: suspend (S) -> Either<ApplicationErrors, S>
) {
    suspend fun handleFlow(request: ServerRequest): ServerResponse =
        ServerResponse.ok().bodyAndAwait(
            flow(inputStream)
                .map {
                    dbSave(it)
                        .flatMap { dbEntity ->
                            esSave(dbEntity.transformFun())
                        }
                }
                .map { either ->
                    either.fold(
                        ifLeft = { errors ->
                            ErrorResponse(errors.joinMessages(), HttpStatus.BAD_REQUEST.value())
                        },
                        ifRight = {
                            it
                        }
                    )
                }
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
    val error: String = "Bad Request", // You can make this more dynamic if needed
    val timestamp: Long = System.currentTimeMillis()
)
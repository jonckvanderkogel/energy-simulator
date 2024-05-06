package com.bullit.energysimulator.controller

import arrow.core.Either
import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.GasConsumption
import com.bullit.energysimulator.PowerConsumption
import com.bullit.energysimulator.csv.gasFlow
import com.bullit.energysimulator.csv.powerFlow
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.joinMessages
import com.bullit.energysimulator.repository.GasConsumptionEntity
import com.bullit.energysimulator.repository.GasConsumptionRepository
import com.bullit.energysimulator.repository.PowerConsumptionEntity
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
        powerConsumptionRepository: PowerConsumptionRepository
    ): PowerHandler {
        val inputStream = javaClass.classLoader.getResourceAsStream("P1e-2024-1-01-2024-5-01.csv")!!
        return PowerHandler(
            inputStream,
            ::powerFlow,
            powerConsumptionRepository::savePowerConsumption
        )
    }

    @Bean
    fun gasHandler(
        gasConsumptionRepository: GasConsumptionRepository
    ): GasHandler {
        val inputStream = javaClass.classLoader.getResourceAsStream("P1g-2024-1-01-2024-5-01.csv")!!
        return GasHandler(
            inputStream,
            ::gasFlow,
            gasConsumptionRepository::saveGasConsumption
        )
    }
}

abstract class RouteHandler<T : Consumption, R : Any>(
    private val inputStream: InputStream,
    private val flow: suspend (InputStream) -> Flow<T>,
    private val save: suspend (T) -> Either<ApplicationErrors, R>
) {
    suspend fun handleFlow(request: ServerRequest): ServerResponse =
        ServerResponse.ok().bodyAndAwait(
            flow(inputStream)
                .map { save(it) }
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
    save: suspend (PowerConsumption) -> Either<ApplicationErrors, PowerConsumptionEntity>
) : RouteHandler<PowerConsumption, PowerConsumptionEntity>(
    inputStream, flow, save
)

class GasHandler(
    inputStream: InputStream,
    flow: suspend (InputStream) -> Flow<GasConsumption>,
    save: suspend (GasConsumption) -> Either<ApplicationErrors, GasConsumptionEntity>
) : RouteHandler<GasConsumption, GasConsumptionEntity>(
    inputStream, flow, save
)

data class ErrorResponse(
    val message: String,
    val status: Int,
    val error: String = "Bad Request", // You can make this more dynamic if needed
    val timestamp: Long = System.currentTimeMillis()
)
package com.bullit.energysimulator.elasticsearch

import arrow.core.Either
import com.bullit.energysimulator.ElasticGasConsumptionEntity
import com.bullit.energysimulator.ElasticPowerConsumptionEntity
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.ElasticsearchInteractionError
import com.bullit.energysimulator.toEither
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

class ElasticsearchService(
    private val ops: ReactiveElasticsearchOperations
) {
    suspend fun savePowerConsumption(
        powerConsumption: ElasticPowerConsumptionEntity
    ): Either<ApplicationErrors, ElasticPowerConsumptionEntity> = ops
        .save(powerConsumption)
        .toEither { t -> ElasticsearchInteractionError(t) }

    suspend fun saveGasConsumption(
        gasConsumption: ElasticGasConsumptionEntity
    ): Either<ApplicationErrors, ElasticGasConsumptionEntity> = ops
        .save(gasConsumption)
        .toEither { t -> ElasticsearchInteractionError(t) }
}
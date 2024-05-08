package com.bullit.energysimulator.repository

import arrow.core.Either
import com.bullit.energysimulator.GasConsumption
import com.bullit.energysimulator.GasConsumptionEntity
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.DatabaseInteractionError
import com.bullit.energysimulator.repository.ReflectionUtil.Companion.deserializeToPojo
import com.bullit.energysimulator.toEither
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GasConsumptionRepository : CoroutineCrudRepository<GasConsumptionEntity, Long>, GasConsumptionRepositoryEntityTemplate

interface GasConsumptionRepositoryEntityTemplate {
    suspend fun saveGasConsumption(gasConsumption: GasConsumption) : Either<ApplicationErrors, GasConsumptionEntity>
}

class GasConsumptionRepositoryEntityTemplateImpl(
    private val template: R2dbcEntityTemplate
) : GasConsumptionRepositoryEntityTemplate {
    override suspend fun saveGasConsumption(gasConsumption: GasConsumption): Either<ApplicationErrors, GasConsumptionEntity> {
        return template.databaseClient
            .sql("INSERT INTO gas_consumption (date_time, amount_consumed) VALUES ($1, $2) RETURNING id, date_time AS \"dateTime\", amount_consumed AS \"amountConsumed\"")
            .bind("$1", gasConsumption.dateTime)
            .bind("$2", gasConsumption.amountConsumed)
            .fetch()
            .first()
            .flatMap {
                deserializeToPojo(it, GasConsumptionEntity::class.java)
            }
            .toEither{ t -> DatabaseInteractionError(t) }
    }
}
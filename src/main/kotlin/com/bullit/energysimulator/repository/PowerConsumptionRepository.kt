package com.bullit.energysimulator.repository

import arrow.core.Either
import com.bullit.energysimulator.PowerConsumption
import com.bullit.energysimulator.PowerConsumptionEntity
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.DatabaseInteractionError
import com.bullit.energysimulator.repository.ReflectionUtil.Companion.deserializeToPojo
import com.bullit.energysimulator.toEither
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/*
These are no longer used. At the start of the project I thought it would be useful to persist the data in Postgres
but as time progressed, it became clear this is not useful at the moment. Keeping it around should it become useful
again in the future.
 */
@Repository
interface PowerConsumptionRepository : CoroutineCrudRepository<PowerConsumptionEntity, Long>, PowerConsumptionRepositoryEntityTemplate

interface PowerConsumptionRepositoryEntityTemplate {
    suspend fun savePowerConsumption(powerConsumption: PowerConsumption) : Either<ApplicationErrors, PowerConsumptionEntity>
}

class PowerConsumptionRepositoryEntityTemplateImpl(
    private val template: R2dbcEntityTemplate
) : PowerConsumptionRepositoryEntityTemplate {
    override suspend fun savePowerConsumption(powerConsumption: PowerConsumption): Either<ApplicationErrors, PowerConsumptionEntity> {
        return template.databaseClient
            .sql("INSERT INTO power_consumption (date_time, amount_consumed, rate) VALUES ($1, $2, $3) RETURNING id, date_time AS \"dateTime\", amount_consumed AS \"amountConsumed\", rate")
            .bind("$1", powerConsumption.dateTime)
            .bind("$2", powerConsumption.amountConsumed)
            .bind("$3", powerConsumption.rate.name)
            .fetch()
            .first()
            .flatMap {
                deserializeToPojo(it, PowerConsumptionEntity::class.java)
            }
            .toEither{ t -> DatabaseInteractionError(t) }
    }
}
package com.bullit.energysimulator.repository

import com.bullit.energysimulator.PowerConsumption
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.stereotype.Repository

@Repository
interface PowerConsumptionRepository : CoroutineCrudRepository<PowerConsumptionEntity, Long>, PowerConsumptionRepositoryEntityTemplate

interface PowerConsumptionRepositoryEntityTemplate {
    suspend fun savePowerConsumption(powerConsumption: PowerConsumption) : PowerConsumptionEntity?
}

class PowerConsumptionRepositoryEntityTemplateImpl(
    private val template: R2dbcEntityTemplate
) : PowerConsumptionRepositoryEntityTemplate {
    override suspend fun savePowerConsumption(powerConsumption: PowerConsumption): PowerConsumptionEntity? {
        return template.databaseClient
            .sql("INSERT INTO power_consumption (date_time, amount_consumed, rate) VALUES ($1, $2, $3) RETURNING id, date_time AS \"dateTime\", amount_consumed AS \"amountConsumed\", rate")
            .bind("$1", powerConsumption.dateTime)
            .bind("$2", powerConsumption.amountConsumed)
            .bind("$3", powerConsumption.rate.name)
            .fetch()
            .awaitOneOrNull()
            ?.let {
                ReflectionUtil.deserializeToPojo(it, PowerConsumptionEntity::class.java)
            }
    }
}
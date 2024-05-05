package com.bullit.energysimulator.repository

import com.bullit.energysimulator.RawCSVDataPower
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.stereotype.Repository

@Repository
interface RawCSVDataPowerRepository : CoroutineCrudRepository<RawCSVDataPowerEntity, Long>, RawCSVDataPowerRepositoryEntityTemplate

interface RawCSVDataPowerRepositoryEntityTemplate {
    suspend fun saveRawCSVDataPower(rawCSVDataPower: RawCSVDataPower) : RawCSVDataPowerEntity?
}

class RawCSVDataPowerRepositoryEntityTemplateImpl(
    private val template: R2dbcEntityTemplate
) : RawCSVDataPowerRepositoryEntityTemplate {
    override suspend fun saveRawCSVDataPower(rawCSVDataPower: RawCSVDataPower) : RawCSVDataPowerEntity? {
        return template.databaseClient
            .sql("INSERT INTO raw_csv_data_power (date_time, t1, t2) VALUES ($1, $2, $3) RETURNING id, date_time AS \"dateTime\", t1, t2")
            .bind("$1", rawCSVDataPower.dateTime)
            .bind("$2", rawCSVDataPower.t1)
            .bind("$3", rawCSVDataPower.t2)
            .fetch()
            .awaitOneOrNull()
            ?.let {
                ReflectionUtil.deserializeToPojo(it, RawCSVDataPowerEntity::class.java)
            }
    }
}
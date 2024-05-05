package com.bullit.energysimulator.repository

import com.bullit.energysimulator.RawCSVDataGas
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.stereotype.Repository

@Repository
interface RawCSVDataGasRepository : CoroutineCrudRepository<RawCSVDataGasEntity, Long>, RawCSVDataGasRepositoryEntityTemplate

interface RawCSVDataGasRepositoryEntityTemplate {
    suspend fun saveRawCSVDataGas(rawCSVDataGas: RawCSVDataGas) : RawCSVDataGasEntity?
}

class RawCSVDataGasRepositoryEntityTemplateImpl(
    private val template: R2dbcEntityTemplate
) : RawCSVDataGasRepositoryEntityTemplate {
    override suspend fun saveRawCSVDataGas(rawCSVDataGas: RawCSVDataGas) : RawCSVDataGasEntity? {
        return template.databaseClient
            .sql("INSERT INTO raw_csv_data_gas (date_time, meter_reading) VALUES ($1, $2) RETURNING id, date_time AS \"dateTime\", meter_reading AS \"meterReading\"")
            .bind("$1", rawCSVDataGas.dateTime)
            .bind("$2", rawCSVDataGas.meterReading)
            .fetch()
            .awaitOneOrNull()
            ?.let {
                ReflectionUtil.deserializeToPojo(it, RawCSVDataGasEntity::class.java)
            }
    }
}
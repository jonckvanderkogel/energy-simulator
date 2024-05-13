package com.bullit.energysimulator

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

interface DbEntity {
    val dateTime: LocalDateTime
    val amountConsumed: Long
}

interface EsEntity {
    val dateTime: LocalDateTime
    fun toDomain(): Consumption
}

@Table("power_consumption")
data class PowerConsumptionEntity(
    @Id val id: Long,
    override val dateTime: LocalDateTime,
    override val amountConsumed: Long,
    val rate: Rate
) : DbEntity

fun PowerConsumptionEntity.toEs(): ElasticPowerConsumptionEntity =
    ElasticPowerConsumptionEntity(this.dateTime, this.amountConsumed, this.rate)

@Table("gas_consumption")
data class GasConsumptionEntity(
    @Id val id: Long,
    override val dateTime: LocalDateTime,
    override val amountConsumed: Long
) : DbEntity

fun GasConsumptionEntity.toEs(): ElasticGasConsumptionEntity =
    ElasticGasConsumptionEntity(this.dateTime, this.amountConsumed)

@Document(indexName = "power_consumption", createIndex = true)
data class ElasticPowerConsumptionEntity(
    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second])
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd'T'HH:mm:ss")
    override val dateTime: LocalDateTime,
    @Field(type = FieldType.Long)
    val powerAmountConsumed: Long,
    @Field(type = FieldType.Keyword)
    val rate: Rate
) : EsEntity {
    override fun toDomain(): Consumption = PowerConsumption(this.dateTime, powerAmountConsumed, rate)
}

@Document(indexName = "gas_consumption", createIndex = true)
data class ElasticGasConsumptionEntity(
    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second])
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd'T'HH:mm:ss")
    override val dateTime: LocalDateTime,
    @Field(type = FieldType.Long)
    val gasAmountConsumed: Long
) : EsEntity {
    override fun toDomain(): Consumption = GasConsumption(this.dateTime, gasAmountConsumed)
}

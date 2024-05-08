package com.bullit.energysimulator

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

interface DbEntity
interface EsEntity

@Table("power_consumption")
data class PowerConsumptionEntity(
    @Id val id: Long,
    val dateTime: LocalDateTime,
    val amountConsumed: Long,
    val rate: Rate
) : DbEntity

fun PowerConsumptionEntity.toEs(): ElasticPowerConsumptionEntity =
    ElasticPowerConsumptionEntity(this.dateTime.atZone(ZoneId.systemDefault()), this.amountConsumed, this.rate)

@Table("gas_consumption")
data class GasConsumptionEntity(
    @Id val id: Long,
    val dateTime: LocalDateTime,
    val amountConsumed: Long
) : DbEntity

fun GasConsumptionEntity.toEs(): ElasticGasConsumptionEntity =
    ElasticGasConsumptionEntity(this.dateTime.atZone(ZoneId.systemDefault()), this.amountConsumed)

@Document(indexName = "power_consumption")
data class ElasticPowerConsumptionEntity(
    @Field(type = FieldType.Date, format = [DateFormat.date_time])
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    val dateTime: ZonedDateTime,
    val powerAmountConsumed: Long,
    val rate: Rate
) : EsEntity

@Document(indexName = "gas_consumption")
data class ElasticGasConsumptionEntity(
    @Field(type = FieldType.Date, format = [DateFormat.date_time])
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    val dateTime: ZonedDateTime,
    val gasAmountConsumed: Long
) : EsEntity

package com.bullit.energysimulator

import com.bullit.energysimulator.controller.ContractType
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
    val amountConsumed: Double
}

@Table("power_consumption")
data class PowerConsumptionEntity(
    @Id val id: Long,
    override val dateTime: LocalDateTime,
    override val amountConsumed: Double,
    val rate: Rate
) : DbEntity

@Table("gas_consumption")
data class GasConsumptionEntity(
    @Id val id: Long,
    override val dateTime: LocalDateTime,
    override val amountConsumed: Double
) : DbEntity

interface EsEntity {
    val dateTime: LocalDateTime
    val amountConsumed: Double
    val cost: Double
    val contractType: ContractType
}

@Document(indexName = "power_consumption", createIndex = true)
data class ElasticPowerConsumptionEntity(
    @Id
    val id: String,
    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second])
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    override val dateTime: LocalDateTime,
    @Field(type = FieldType.Double)
    override val amountConsumed: Double,
    @Field(type = FieldType.Keyword)
    val rate: Rate,
    @Field(type = FieldType.Double)
    override val cost: Double,
    @Field(type = FieldType.Keyword)
    override val contractType: ContractType
) : EsEntity {
    constructor(
        dateTime: LocalDateTime,
        amountConsumed: Double,
        rate: Rate,
        cost: Double,
        contractType: ContractType
    ) : this(
        id = "$dateTime-$contractType",
        dateTime = dateTime,
        amountConsumed = amountConsumed,
        rate = rate,
        cost = cost,
        contractType = contractType
    )
}

fun PowerConsumption.toElasticPowerConsumption(
    cost: Double,
    contractType: ContractType
): ElasticPowerConsumptionEntity =
    ElasticPowerConsumptionEntity(
        this.dateTime,
        this.amountConsumed,
        this.rate,
        cost,
        contractType
    )

@Document(indexName = "gas_consumption", createIndex = true)
data class ElasticGasConsumptionEntity(
    @Id
    val id: String,
    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second])
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    override val dateTime: LocalDateTime,
    @Field(type = FieldType.Double)
    override val amountConsumed: Double,
    @Field(type = FieldType.Double)
    override val cost: Double,
    @Field(type = FieldType.Keyword)
    override val contractType: ContractType
) : EsEntity {
    constructor(
        dateTime: LocalDateTime,
        amountConsumed: Double,
        cost: Double,
        contractType: ContractType
    ) : this(
        id = "$dateTime-$contractType",
        dateTime = dateTime,
        amountConsumed = amountConsumed,
        cost = cost,
        contractType = contractType
    )
}

fun GasConsumption.toElasticGasConsumption(
    cost: Double,
    contractType: ContractType
): ElasticGasConsumptionEntity =
    ElasticGasConsumptionEntity(
        this.dateTime,
        this.amountConsumed,
        cost,
        contractType
    )
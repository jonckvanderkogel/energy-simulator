package com.bullit.energysimulator

import arrow.core.Either
import arrow.core.leftNel
import arrow.core.right
import com.bullit.energysimulator.EnergyType.*
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.InvalidParameterError
import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDateTime

sealed interface EsEntity {
    val dateTime: LocalDateTime
    val amountConsumed: Double
    val cost: Double
    val energySourceType: EnergySourceType
    val energyType: EnergyType
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
    override val energySourceType: EnergySourceType,
    @Field(type = FieldType.Keyword)
    override val energyType: EnergyType = POWER,
    @Field(type = FieldType.Keyword)
    val powerConsumptionType: PowerConsumptionType
) : EsEntity {
    constructor(
        dateTime: LocalDateTime,
        amountConsumed: Double,
        rate: Rate,
        cost: Double,
        energySourceType: EnergySourceType,
        powerConsumptionType: PowerConsumptionType
    ) : this(
        id = "$dateTime-$energySourceType-$powerConsumptionType",
        dateTime = dateTime,
        amountConsumed = amountConsumed,
        rate = rate,
        cost = cost,
        energySourceType = energySourceType,
        powerConsumptionType = powerConsumptionType
    )
}

fun PowerConsumption.toElasticPowerConsumption(
    cost: Double,
    contractType: EnergySourceType,
    powerConsumptionType: PowerConsumptionType
): ElasticPowerConsumptionEntity =
    ElasticPowerConsumptionEntity(
        this.dateTime,
        this.amountConsumed,
        this.rate,
        cost,
        contractType,
        powerConsumptionType
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
    override val energySourceType: EnergySourceType,
    @Field(type = FieldType.Keyword)
    override val energyType: EnergyType = GAS
) : EsEntity {
    constructor(
        dateTime: LocalDateTime,
        amountConsumed: Double,
        cost: Double,
        energySourceType: EnergySourceType
    ) : this(
        id = "$dateTime-$energySourceType",
        dateTime = dateTime,
        amountConsumed = amountConsumed,
        cost = cost,
        energySourceType = energySourceType
    )
}

fun GasConsumption.toElasticGasConsumption(
    cost: Double,
    contractType: EnergySourceType
): ElasticGasConsumptionEntity =
    ElasticGasConsumptionEntity(
        this.dateTime,
        this.amountConsumed,
        cost,
        contractType
    )

interface ParsableEnum<T : Enum<T>> {
    val type: String

    companion object {
        inline fun <reified T> parse(enumValue: String): Either<ApplicationErrors, T> where T : Enum<T>, T : ParsableEnum<T> =
            try {
                val enumConstant = enumValueOf<T>(enumValue.uppercase())
                enumConstant.right()
            } catch (e: IllegalArgumentException) {
                val type = enumValues<T>().firstOrNull()?.type ?: "unknown"
                InvalidParameterError(enumValue, type).leftNel()
            }
    }
}

enum class EnergySourceType(override val type: String) : ParsableEnum<EnergySourceType> {
    FIXED("source"), DYNAMIC("source"), BATTERY("source");

    companion object : ParsableEnum<EnergySourceType> {
        override val type: String = "source"

        fun parse(energySourceType: String): Either<ApplicationErrors, EnergySourceType> =
            ParsableEnum.parse(energySourceType)
    }
}

enum class HeatingType(override val type: String) : ParsableEnum<HeatingType> {
    BOILER("heating"), HEATPUMP("heating");

    companion object : ParsableEnum<HeatingType> {
        override val type: String = "heating"

        fun parse(heatingType: String): Either<ApplicationErrors, HeatingType> =
            ParsableEnum.parse(heatingType)
    }
}

enum class PowerConsumptionType {
    GENERAL, HEATING
}

enum class EnergyType {
    POWER, GAS
}
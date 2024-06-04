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
    companion object {
        inline fun <reified T> parse(paramValue: String, paramName: String): Either<ApplicationErrors, T> where T : Enum<T>, T : ParsableEnum<T> =
            try {
                val enumConstant = enumValueOf<T>(paramValue.uppercase())
                enumConstant.right()
            } catch (e: IllegalArgumentException) {
                InvalidParameterError(paramValue, paramName).leftNel()
            }
    }
}

enum class EnergySourceType : ParsableEnum<EnergySourceType> {
    FIXED, DYNAMIC, BATTERY;

    companion object : ParsableEnum<EnergySourceType> {
        fun parse(energySourceType: String, paramName: String): Either<ApplicationErrors, EnergySourceType> =
            ParsableEnum.parse(energySourceType, paramName)
    }
}

enum class HeatingType : ParsableEnum<HeatingType> {
    BOILER, HEATPUMP, NOT_HEATING;

    companion object : ParsableEnum<HeatingType> {
        fun parse(heatingType: String, paramName: String): Either<ApplicationErrors, HeatingType> =
            ParsableEnum.parse(heatingType, paramName)
    }
}

enum class PowerConsumptionType {
    GENERAL, HEATING
}

enum class EnergyType {
    POWER, GAS
}
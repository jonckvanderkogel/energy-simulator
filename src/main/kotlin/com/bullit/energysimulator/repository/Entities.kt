package com.bullit.energysimulator.repository

import com.bullit.energysimulator.*
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime


@Table("power_consumption")
data class PowerConsumptionEntity(
    @Id val id: Long,
    val dateTime: LocalDateTime,
    val amountConsumed: Long,
    val rate: Rate
)

fun PowerConsumptionEntity.toDomain(): PowerConsumption =
    PowerConsumption(this.dateTime, this.amountConsumed, this.rate)

@Table("gas_consumption")
data class GasConsumptionEntity(
    @Id val id: Long,
    val dateTime: LocalDateTime,
    val amountConsumed: Long
)

fun GasConsumptionEntity.toDomain(): GasConsumption =
    GasConsumption(this.dateTime, this.amountConsumed)

@Table("raw_csv_data_power")
data class RawCSVDataPowerEntity(
    @Id val id: Long,
    val dateTime: LocalDateTime,
    val t1: Long,
    val t2: Long
)

fun RawCSVDataPowerEntity.toDomain(): RawCSVDataPower =
    RawCSVDataPower(this.dateTime, this.t1, this.t2)

@Table("raw_csv_data_gas")
data class RawCSVDataGasEntity(
    @Id val id: Long,
    val dateTime: LocalDateTime,
    val meterReading: Long
)

fun RawCSVDataGasEntity.toDomain(): RawCSVDataGas =
    RawCSVDataGas(this.dateTime, this.meterReading)
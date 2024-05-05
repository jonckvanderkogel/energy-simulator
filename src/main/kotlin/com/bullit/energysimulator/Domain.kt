package com.bullit.energysimulator

import java.time.LocalDateTime

interface Consumption{
    val dateTime: LocalDateTime
    val amountConsumed: Long
}

data class PowerConsumption(
    override val dateTime: LocalDateTime,
    override val amountConsumed: Long,
    val rate: Rate
): Consumption

data class GasConsumption(
    override val dateTime: LocalDateTime,
    override val amountConsumed: Long
): Consumption

interface RawCSVData {
    val dateTime: LocalDateTime
}

data class RawCSVDataPower(
    override val dateTime: LocalDateTime,
    val t1: Long,
    val t2: Long
): RawCSVData

data class RawCSVDataGas(
    override val dateTime: LocalDateTime,
    val meterReading: Long
): RawCSVData

enum class Rate {
    T1, T2
}
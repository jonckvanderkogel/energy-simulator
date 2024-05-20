package com.bullit.energysimulator

import arrow.core.Either
import arrow.core.leftNel
import arrow.core.right
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.MissingArgumentError
import java.time.LocalDateTime

sealed interface Consumption {
    val dateTime: LocalDateTime
    val amountConsumed: Double
}

data class PowerConsumption(
    override val dateTime: LocalDateTime,
    override val amountConsumed: Double,
    val rate: Rate
): Consumption

data class GasConsumption(
    override val dateTime: LocalDateTime,
    override val amountConsumed: Double
): Consumption

interface RawCSVData {
    val dateTime: LocalDateTime
}

data class RawCSVDataPower(
    override val dateTime: LocalDateTime,
    val t1: Double,
    val t2: Double
): RawCSVData

data class RawCSVDataGas(
    override val dateTime: LocalDateTime,
    val meterReading: Double
): RawCSVData

enum class Rate {
    T1, T2
}

enum class ContractType() {
    FIXED, DYNAMIC;

    companion object {
        fun parseContractTypeString(contractTypeString: String): Either<ApplicationErrors, ContractType> =
            try {
                ContractType.valueOf(contractTypeString.uppercase()).right()
            } catch (e: IllegalArgumentException) {
                MissingArgumentError("ContractType: $contractTypeString does not exist").leftNel()
            }
    }
}
package com.bullit.energysimulator.csv

import com.bullit.energysimulator.PowerConsumption
import com.bullit.energysimulator.Rate
import com.bullit.energysimulator.RawCSVDataPower
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

private fun powerConsumptionCalculator(): ConsumptionCalculator<RawCSVDataPower, PowerConsumption> =
    ConsumptionCalculator { previousRawData, newRawData ->
        previousRawData?.let { csvDataPower ->
            when {
                newRawData.t1 > csvDataPower.t1 && newRawData.t2 > csvDataPower.t2 -> newRawData.t1 - csvDataPower.t1 + newRawData.t2 - csvDataPower.t2 to Rate.T1
                newRawData.t1 > csvDataPower.t1 -> newRawData.t1 - csvDataPower.t1 to Rate.T1
                newRawData.t2 > csvDataPower.t2 -> newRawData.t2 - csvDataPower.t2 to Rate.T2
                else -> null
            }?.let {
                PowerConsumption(newRawData.dateTime, it.first, it.second)
            }
        }
    }

private fun powerCSVParser(
    formatter: DateTimeFormatter
): RawCSVParser<RawCSVDataPower> = RawCSVParser { record ->
    RawCSVDataPower(
        LocalDateTime.parse(record[0], formatter),
        (record[1].toDouble() * 1000).roundToLong(),
        (record[2].toDouble() * 1000).roundToLong()
    )
}

suspend fun powerFlow(csvInputStream: InputStream): Flow<PowerConsumption> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val powerCSVParser = powerCSVParser(formatter)

    return calculateConsumption(
        csvInputStream,
        powerCSVParser,
        powerConsumptionCalculator()
    )
}
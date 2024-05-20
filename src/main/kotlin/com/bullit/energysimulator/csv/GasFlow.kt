package com.bullit.energysimulator.csv

import com.bullit.energysimulator.GasConsumption
import com.bullit.energysimulator.RawCSVDataGas
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun gasConsumptionCalculator(): ConsumptionCalculator<RawCSVDataGas, GasConsumption> =
    ConsumptionCalculator { previousRawData, newRawData ->
        previousRawData?.let { pd ->
            GasConsumption(newRawData.dateTime, newRawData.meterReading - pd.meterReading)
        }
    }

private fun gasCSVParser(
    formatter: DateTimeFormatter
): RawCSVParser<RawCSVDataGas> = RawCSVParser { record ->
    RawCSVDataGas(
        LocalDateTime.parse(record[0], formatter),
        record[1].toDouble()
    )
}

suspend fun gasFlow(
    csvInputStream: InputStream
): Flow<GasConsumption> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val gasCSVParser = gasCSVParser(formatter)

    return calculateConsumption(
        csvInputStream,
        gasCSVParser,
        gasConsumptionCalculator()
    )
}

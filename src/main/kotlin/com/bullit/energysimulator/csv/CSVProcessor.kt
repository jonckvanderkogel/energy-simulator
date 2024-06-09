package com.bullit.energysimulator.csv

import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.RawCSVData
import kotlinx.coroutines.flow.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.InputStream

fun interface ConsumptionCalculator<in T : RawCSVData, out R : Consumption> {
    fun calculateConsumption(previousRawData: T?, newRawData: T): R?
}

fun interface RawCSVParser<out T : RawCSVData> {
    fun parseCSV(record: CSVRecord): T
}

class ConsumptionProcessor<in T : RawCSVData, out R : Consumption>
    (private val consumptionCalculator: ConsumptionCalculator<T, R>) {

    private var previousData: T? = null

    fun processData(newData: T): R? {
        val previousData = this.previousData
        this.previousData = newData
        return consumptionCalculator.calculateConsumption(previousData, newData)
    }
}

suspend fun <T : RawCSVData> processCSV(
    inputStream: InputStream,
    rawCSVParser: RawCSVParser<T>
): Flow<T> = flow {
    val csvFormat = CSVFormat.Builder.create()
        .setHeader()
        .setSkipHeaderRecord(true)
        .build()
    val parser = CSVParser.parse(inputStream, Charsets.UTF_8, csvFormat)

    parser.records.forEach { record ->
        val rawCSVData = rawCSVParser.parseCSV(record)
        emit(rawCSVData)
    }
}

suspend inline fun <reified T : RawCSVData, R : Consumption> calculateConsumption(
    csvInputStream: InputStream,
    rawCSVParser: RawCSVParser<T>,
    consumptionCalculator: ConsumptionCalculator<T, R>
): Flow<R> {
    val consumptionProcessor = ConsumptionProcessor(consumptionCalculator)
    return processCSV(csvInputStream, rawCSVParser)
        .map { rawCsvData ->
            consumptionProcessor.processData(rawCsvData)
        }
        .filterNotNull()
}

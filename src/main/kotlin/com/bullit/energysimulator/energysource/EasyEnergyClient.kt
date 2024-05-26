package com.bullit.energysimulator.energysource

import arrow.core.Either
import arrow.core.leftNel
import arrow.core.right
import com.bullit.energysimulator.Resilience4jConfiguration.Companion.EASY_ENERGY_CLIENT
import com.bullit.energysimulator.errorhandling.AbstractApplicationError
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.EasyEnergyApiInteractionError
import com.bullit.energysimulator.toEither
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


class EasyEnergyClient(
    retryRegistry: RetryRegistry,
    baseUrl: String,
    private val powerEndpoint: String,
    private val gasEndpoint: String
) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    private val retry = retryRegistry.retry(EASY_ENERGY_CLIENT)

    private fun fetchEnergyPricesReactive(energyEndpoint: String, date: LocalDate): Mono<List<EnergyTariff>> {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val startTimestamp = date.atStartOfDay().format(formatter) + "T00:00:00.000Z"
        val endTimestamp = date.plusDays(1).atStartOfDay().format(formatter) + "T00:00:00.000Z"
        val uri = "/$energyEndpoint?startTimestamp=$startTimestamp&endTimestamp=$endTimestamp&includeVat=true"

        return webClient
            .get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<EnergyTariffDTO>>() {})
            .map { list ->
                list.map {
                    it.toEnergyTariff()
                }
            }
            .transformDeferred(RetryOperator.of(retry))
            .onErrorResume { fallback(it) }
    }

    private fun fallback(e: Throwable): Mono<List<EnergyTariff>> {
        return Mono.error(RuntimeException("Tried too many times", e))
    }

    private suspend fun fetchEnergyPrices(endpointType: String, date: LocalDate): Either<ApplicationErrors, List<EnergyTariff>> =
        fetchEnergyPricesReactive(endpointType, date)
            .toEither { t -> EasyEnergyApiInteractionError(t) }

    suspend fun fetchPowerPrices(date: LocalDate): Either<ApplicationErrors, List<EnergyTariff>> =
        fetchEnergyPrices(powerEndpoint, date)

    suspend fun fetchGasPrices(date: LocalDate): Either<ApplicationErrors, List<EnergyTariff>> =
        fetchEnergyPrices(gasEndpoint, date)
}

private data class EnergyTariffDTO(
    @JsonProperty("Timestamp") val timestamp: OffsetDateTime,
    @JsonProperty("SupplierId") val supplierId: Int,
    @JsonProperty("TariffUsage") val tariffUsage: Double,
    @JsonProperty("TariffReturn") val tariffReturn: Double
)

private fun EnergyTariffDTO.toEnergyTariff() = EnergyTariff(timestamp.toLocalDateTime(), tariffUsage, tariffReturn)

data class EnergyTariff(
    val dateTime: LocalDateTime,
    val rateUsage: Double,
    val rateReturn: Double
)

fun EnergyTariff?.toEither(errorFun: () -> AbstractApplicationError): Either<ApplicationErrors, EnergyTariff> =
    this?.right() ?: errorFun().leftNel()

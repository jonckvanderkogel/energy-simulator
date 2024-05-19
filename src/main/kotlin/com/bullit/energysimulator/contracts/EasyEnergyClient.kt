package com.bullit.energysimulator.contracts

import arrow.core.Either
import com.bullit.energysimulator.Resilience4jConfiguration.Companion.EASY_ENERGY_CLIENT
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.EasyEnergyApiInteractionError
import com.bullit.energysimulator.toEither
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.xml.Jaxb2XmlDecoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


class EasyEnergyClient(
    retryRegistry: RetryRegistry,
    baseUrl: String
) {
    private val webClient = WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs{ configure ->
                    configure.customCodecs().register(Jaxb2XmlDecoder())
                }
                .build()
        )
        .baseUrl(baseUrl)
        .build()

    private val retry = retryRegistry.retry(EASY_ENERGY_CLIENT)

    private fun fetchEnergyPricesReactive(date: LocalDate): Mono<List<EnergyTariff>> {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val startTimestamp = date.atStartOfDay().format(formatter) + "T00:00:00.000Z"
        val endTimestamp = date.plusDays(1).atStartOfDay().format(formatter) + "T00:00:00.000Z"
        val uri = "/getapxtariffs?startTimestamp=$startTimestamp&endTimestamp=$endTimestamp"

        return webClient
            .get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<EnergyTariff>>() {})
            .transformDeferred(RetryOperator.of(retry))
            .onErrorResume { fallback(it) }
    }

    private fun fallback(e: Throwable): Mono<List<EnergyTariff>> {
        return Mono.error(RuntimeException("Tried too many times", e))
    }

    suspend fun fetchEnergyPrices(date: LocalDate): Either<ApplicationErrors, List<EnergyTariff>> =
        fetchEnergyPricesReactive(date)
            .toEither { t -> EasyEnergyApiInteractionError(t) }
}

data class EnergyTariff(
    @JsonProperty("Timestamp") val timestamp: OffsetDateTime,
    @JsonProperty("SupplierId") val supplierId: Int,
    @JsonProperty("TariffUsage") val tariffUsage: Double,
    @JsonProperty("TariffReturn") val tariffReturn: Double
)

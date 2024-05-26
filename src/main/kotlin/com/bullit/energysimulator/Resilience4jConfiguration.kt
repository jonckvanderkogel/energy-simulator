package com.bullit.energysimulator

import com.bullit.energysimulator.energysource.EnergyTariff
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClientRequestException
import java.io.IOException

@Configuration
class Resilience4jConfiguration {
    companion object {
        const val EASY_ENERGY_CLIENT = "easyEnergyClient"
    }

    @Bean
    fun retryRegistry(): RetryRegistry {
        val easyEnergyRetryConfig = RetryConfig.custom<List<EnergyTariff>>()
            .maxAttempts(5)
            .retryExceptions(IOException::class.java, WebClientRequestException::class.java)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(100, 2.0))
            .build()

        return RetryRegistry.of(
            mapOf(
                EASY_ENERGY_CLIENT to easyEnergyRetryConfig
            )
        )
    }

    @Bean
    fun retryLogger(retryRegistry: RetryRegistry) = RetryLogger(retryRegistry)
}

class RetryLogger(
    retryRegistry: RetryRegistry
) {
    companion object {
        private val logger = logger()
    }

    init {
        retryRegistry
            .allRetries
            .map { retry ->
                retry
                    .eventPublisher
                    .onRetry {
                        logger.info("Retrying: $it")
                    }
            }
    }
}

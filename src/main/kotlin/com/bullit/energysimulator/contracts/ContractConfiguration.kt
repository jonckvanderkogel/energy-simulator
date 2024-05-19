package com.bullit.energysimulator.contracts

import io.github.resilience4j.retry.RetryRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ContractConfiguration {
    @Bean
    fun easyEnergyClient(
        retryRegistry: RetryRegistry,
        @Value("\${easyEnergy.url}") baseUrl: String
    ): EasyEnergyClient = EasyEnergyClient(retryRegistry, baseUrl)

    @Bean
    @ConditionalOnProperty(name = ["contract.type"], havingValue = "FIXED")
    fun fixedContract(
        @Value("\${contract.fixed.power.t1}") powerPriceT1: Double,
        @Value("\${contract.fixed.power.t2}") powerPriceT2: Double,
        @Value("\${contract.fixed.gas}") gasPrice: Double,
    ): EnergyContract = FixedContract(
        powerPriceT1, powerPriceT2, gasPrice
    )
}
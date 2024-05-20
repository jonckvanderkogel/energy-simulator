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
        @Value("\${easyEnergy.url}") baseUrl: String,
        @Value("\${easyEnergy.power}") powerEndpoint: String,
        @Value("\${easyEnergy.gas}") gasEndpoint: String,
    ): EasyEnergyClient = EasyEnergyClient(retryRegistry, baseUrl, powerEndpoint, gasEndpoint)

    @Bean
    fun fixedContract(
        @Value("\${contract.fixed.power.t1}") powerPriceT1: Double,
        @Value("\${contract.fixed.power.t2}") powerPriceT2: Double,
        @Value("\${contract.fixed.gas}") gasPrice: Double,
    ): EnergyContract = FixedContract(
        powerPriceT1, powerPriceT2, gasPrice
    )

    @Bean
    fun dynamicContract(
        easyEnergyClient: EasyEnergyClient
    ): EnergyContract = DynamicContract(easyEnergyClient)
}
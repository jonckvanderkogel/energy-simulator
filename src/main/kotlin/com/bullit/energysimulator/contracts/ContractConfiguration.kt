package com.bullit.energysimulator.contracts

import com.bullit.energysimulator.Consumption
import com.bullit.energysimulator.controller.ContractType
import com.bullit.energysimulator.controller.ContractType.*
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.beans.factory.annotation.Value
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
    ): FixedContract = FixedContract(
        powerPriceT1, powerPriceT2, gasPrice
    )

    @Bean
    fun dynamicContract(
        easyEnergyClient: EasyEnergyClient
    ): DynamicContract = DynamicContract(easyEnergyClient)

    @Bean
    fun energyContractProvider(
        fixedContract: FixedContract,
        dynamicContract: DynamicContract
    ): EnergyContractProvider<Consumption> = { contractType ->
        when (contractType) {
            FIXED -> fixedContract
            DYNAMIC -> dynamicContract
        }
    }
}

typealias EnergyContractProvider<T> = (ContractType) -> EnergyContract<T>
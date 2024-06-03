package com.bullit.energysimulator.energysource

import arrow.core.Either
import com.bullit.energysimulator.EnergySourceType
import com.bullit.energysimulator.EnergySourceType.*
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.resilience4j.retry.RetryRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate

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
        scop: SCOP
    ): FixedContract = FixedContract(
        powerPriceT1, powerPriceT2, gasPrice, scop
    )

    @Bean
    fun dynamicContract(
        @Qualifier("powerTariffCache") powerTariffCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>>,
        @Qualifier("gasTariffCache") gasTariffCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>>,
        @Value("\${tax.power}") taxPower: Double,
        @Value("\${tax.gas}") taxGas: Double,
        scop: SCOP
    ): DynamicContract = DynamicContract(
        taxPower, taxGas, scop, powerTariffCache, gasTariffCache
    )

    @Bean
    fun battery(
        @Qualifier("powerTariffCache") powerTariffCache: AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>>,
        @Value("\${tax.power}") taxPower: Double,
        dynamicContract: DynamicContract
    ): Battery = Battery(
        powerTariffCache,
        dynamicContract
    )

    @Bean
    fun energySourceProvider(
        fixedContract: FixedContract,
        dynamicContract: DynamicContract,
        battery: Battery
    ): EnergySourceProvider = { contractType ->
        when (contractType) {
            FIXED -> fixedContract
            DYNAMIC -> dynamicContract
            BATTERY -> battery
        }
    }

    @Bean("powerTariffCache")
    fun powerTariffCache(
            easyEnergyClient: EasyEnergyClient
    ): AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>> =
        buildCache(easyEnergyClient::fetchPowerPrices)

    @Bean("gasTariffCache")
    fun gasTariffCache(
        easyEnergyClient: EasyEnergyClient
    ): AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>> =
        buildCache(easyEnergyClient::fetchGasPrices)

    @Bean
    fun scop(
        @Value("\${scop}") scop: Double,
    ): SCOP = SCOP(scop)

    @JvmInline
    value class SCOP(val scopValue: Double)

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun buildCache(
        easyEnergyClientFun: suspend (date: LocalDate) -> Either<ApplicationErrors, List<EnergyTariff>>
    ): AsyncLoadingCache<LocalDate, Either<ApplicationErrors, List<EnergyTariff>>> =
        Caffeine.newBuilder()
            .buildAsync { key, _ ->
                scope.async {
                    easyEnergyClientFun(key)
                }.asCompletableFuture()
            }
}

typealias EnergySourceProvider = (EnergySourceType) -> EnergySource
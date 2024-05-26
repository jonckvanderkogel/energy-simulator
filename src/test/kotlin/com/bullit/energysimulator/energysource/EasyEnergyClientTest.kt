package com.bullit.energysimulator.energysource

import com.bullit.energysimulator.AbstractWiremockTest
import com.bullit.energysimulator.Resilience4jConfiguration
import com.bullit.energysimulator.wiremock.WireMockProxy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [ContractConfiguration::class, Resilience4jConfiguration::class, WireMockProxy::class])
class EasyEnergyClientTest(
    @Autowired private val proxy: WireMockProxy,
    @Autowired private val easyEnergyClient: EasyEnergyClient
) : AbstractWiremockTest(proxy) {
    @Test
    fun `easyEnergyClient should get dynamic energy price information`() {
        val result = runBlocking {
            easyEnergyClient.fetchPowerPrices(LocalDate.of(2024, 1, 1))
        }
        assertTrue(result.isRight())

        result
            .map {
                assertEquals(24, it.size)
                assertEquals(0.0000121, it[0].rateUsage)
            }
    }
}
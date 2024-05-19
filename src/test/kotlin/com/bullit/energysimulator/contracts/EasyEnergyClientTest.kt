package com.bullit.energysimulator.contracts

import com.bullit.energysimulator.Resilience4jConfiguration
import com.bullit.energysimulator.wiremock.WireMockProxy
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.recording.RecordSpec
import com.github.tomakehurst.wiremock.recording.RecordingStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [ContractConfiguration::class, Resilience4jConfiguration::class, WireMockProxy::class])
class EasyEnergyClientTest(
    @Autowired private val proxy: WireMockProxy,
    @Autowired private val easyEnergyClient: EasyEnergyClient
) {
    private lateinit var wiremockServer: WireMockServer

    private val getMockServer: (WireMockProxy) -> WireMockServer = { proxy ->
        WireMockServer(
            WireMockConfiguration
                .options()
                .port(proxy.port)
                .withRootDirectory("src/test/resources")
                .notifier(ConsoleNotifier(true))
        )
    }

    @BeforeEach
    fun startRecording() {
        val wireMockServer = getMockServer(proxy)
        wireMockServer.start()
        if (proxy.recording) wireMockServer.startRecording(config(proxy.url))
        wiremockServer = wireMockServer
    }

    @AfterEach
    fun stopRecording() {
        if (wiremockServer.recordingStatus.status == RecordingStatus.Recording) wiremockServer.stopRecording()
        wiremockServer.stop()
    }

    private fun config(recordingURL: String): RecordSpec =
        WireMock.recordSpec()
            .forTarget(recordingURL)
            .onlyRequestsMatching(RequestPatternBuilder.allRequests())
            .captureHeader("Accept")
            .makeStubsPersistent(true)
            .build()

    @Test
    fun `easyEnergyClient should get dynamic energy price information`() {
        val result = runBlocking {
            easyEnergyClient.fetchEnergyPrices(LocalDate.of(2024, 1, 1))
        }
        println(result.getOrNull())
        assertTrue(result.isRight())
    }
}
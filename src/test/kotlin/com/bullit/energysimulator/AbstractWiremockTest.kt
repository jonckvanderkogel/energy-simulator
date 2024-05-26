package com.bullit.energysimulator

import com.bullit.energysimulator.wiremock.WireMockProxy
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.recording.RecordSpec
import com.github.tomakehurst.wiremock.recording.RecordingStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class AbstractWiremockTest(
    private val proxy: WireMockProxy
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
}
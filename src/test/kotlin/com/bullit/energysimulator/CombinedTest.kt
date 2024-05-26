package com.bullit.energysimulator

import com.bullit.energysimulator.wiremock.WireMockProxy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

abstract class CombinedTest(
    @Autowired private val proxy: WireMockProxy
) : AbstractIntegrationTest() {
    private val wiremockTest = object : AbstractWiremockTest(proxy) {}

    @BeforeEach
    fun startRecording() {
        wiremockTest.startRecording()
    }

    @AfterEach
    fun stopRecording() {
        wiremockTest.stopRecording()
    }
}
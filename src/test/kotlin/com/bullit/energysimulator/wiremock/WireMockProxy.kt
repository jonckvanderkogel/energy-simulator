package com.bullit.energysimulator.wiremock

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Component

@PropertySource("classpath:application.yml")
@Component
data class WireMockProxy(
    @Value("\${wiremock-config.url}") val url: String,
    @Value("\${wiremock-config.port}") val port: Int,
    @Value("\${wiremock-config.recording}") val recording: Boolean
)
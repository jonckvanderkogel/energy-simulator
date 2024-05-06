package com.bullit.energysimulator.controller

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class Routes {
    @Bean
    fun powerRoute(powerHandler: PowerHandler) = coRouter {
        GET("/import/power", powerHandler::handleFlow)
    }

    @Bean
    fun gasRoute(gasHandler: GasHandler) = coRouter {
        GET("/import/gas", gasHandler::handleFlow)
    }
}
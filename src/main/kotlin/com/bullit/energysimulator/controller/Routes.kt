package com.bullit.energysimulator.controller

import com.bullit.energysimulator.ElasticPowerConsumptionEntity
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

    /*
    curl "http://localhost:8080/search?gte=2024-03-31T22:00&lte=2024-03-31T22:45"
     */
    @Bean
    fun searchRoute(searchHandler: SearchHandler<ElasticPowerConsumptionEntity>) = coRouter {
        GET("/search", searchHandler::search)
    }
}
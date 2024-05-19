package com.bullit.energysimulator.contracts

import java.time.LocalDateTime

interface EnergyContract {
    fun powerPrice(dateTime: LocalDateTime): Double
    fun gasPrice(dateTime: LocalDateTime): Double
}
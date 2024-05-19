package com.bullit.energysimulator.contracts

import java.time.LocalDateTime

class FixedContract(
    private val powerPriceT1: Double,
    private val powerPriceT2: Double,
    private val gasPrice: Double,
) : EnergyContract {
    override fun powerPrice(dateTime: LocalDateTime): Double =
        when {
            dateTime.hour in 22..23 || dateTime.hour in 0..6 -> powerPriceT2
            else -> powerPriceT1
        }

    override fun gasPrice(dateTime: LocalDateTime): Double = gasPrice
}
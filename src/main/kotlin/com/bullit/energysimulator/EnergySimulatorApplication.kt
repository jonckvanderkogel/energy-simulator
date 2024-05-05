package com.bullit.energysimulator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EnergySimulatorApplication

fun main(args: Array<String>) {
	runApplication<EnergySimulatorApplication>(*args)
}

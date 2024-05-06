package com.bullit.energysimulator.repository

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import reactor.core.publisher.Mono

class ReflectionUtil {
    companion object {
        private val OBJECT_MAPPER = jacksonObjectMapper()
            .registerModule(JavaTimeModule())

        fun <T : Any> deserializeToPojo(map: Map<String, Any>, clazz: Class<T>): Mono<T> {
            return try {
                Mono.just(OBJECT_MAPPER.convertValue(map, clazz))
            } catch (ex: Exception) {
                Mono.error(ex)
            }
        }
    }
}
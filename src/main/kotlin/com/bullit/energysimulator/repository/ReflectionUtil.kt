package com.bullit.energysimulator.repository

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class ReflectionUtil {
    companion object {
        private val OBJECT_MAPPER = jacksonObjectMapper()
            .registerModule(JavaTimeModule())

        fun <T : Any> deserializeToPojo(map: Map<String, Any>, clazz: Class<T>): T? {
            return try {
                OBJECT_MAPPER.convertValue(map, clazz)
            } catch (ex: Exception) {
                null
            }
        }
    }
}
package com.bullit.energysimulator.elasticsearch

import arrow.core.Either
import co.elastic.clients.json.JsonData
import com.bullit.energysimulator.EsEntity
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.ElasticsearchInteractionError
import com.bullit.energysimulator.toEither
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ElasticsearchService(
    val ops: ReactiveElasticsearchOperations
) {
    suspend inline fun <reified T : EsEntity> saveConsumption(
        consumption: T
    ): Either<ApplicationErrors, T> = ops
        .save(consumption)
        .toEither { t -> ElasticsearchInteractionError(t) }

    inline fun <reified T: EsEntity> searchByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<T> {
        val dateRangeQuery = NativeQuery.builder()
            .withQuery { query ->
                query.range { range ->
                    range.field("dateTime")
                        .gte(JsonData.of(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                        .lte(JsonData.of(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                }
            }.build()

        return ops
            .search(dateRangeQuery, T::class.java)
            .map { it.content }
            .asFlow()
    }
}
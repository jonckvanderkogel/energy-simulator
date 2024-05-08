package com.bullit.energysimulator.elasticsearch

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@Configuration
class ElasticsearchConfiguration(
    @Value("\${elasticsearch.url}") private val elasticsearchUrl: String
) : ReactiveElasticsearchConfiguration() {
    override fun clientConfiguration(): ClientConfiguration =
        ClientConfiguration
            .builder()
            .connectedTo(elasticsearchUrl)
            .build()

    @Bean
    fun elasticsearchService(
        ops: ReactiveElasticsearchOperations
    ): ElasticsearchService = ElasticsearchService(ops)
}
package com.bullit.energysimulator.elasticsearch

import com.bullit.energysimulator.AbstractIntegrationTest
import com.bullit.energysimulator.ElasticPowerConsumptionEntity
import com.bullit.energysimulator.Rate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime

@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@SpringBootTest
class ElasticSearchServiceTest(
    @Autowired val elasticsearchService: ElasticsearchService,
    @Autowired val ops: ReactiveElasticsearchOperations
) : AbstractIntegrationTest() {

    @Order(1)
    @Test
    fun `should persist power consumption`() {
        val consumption = ElasticPowerConsumptionEntity(
            LocalDateTime.of(2024, 3, 31, 22, 30),
            10L,
            Rate.T1
        )

        val persisted = runBlocking {
            elasticsearchService.saveConsumption(consumption)
        }

        assertTrue(persisted.isRight())
        assertEquals(10L, persisted.getOrNull()?.powerAmountConsumed)
    }

    @Order(2)
    @Test
    fun `should fetch power consumption`() {
        runBlocking {
            ops
                .indexOps(ElasticPowerConsumptionEntity::class.java)
                .refresh()
                .awaitFirstOrNull()
        }

        val retrieved = runBlocking {
            elasticsearchService
                .searchByDateRange<ElasticPowerConsumptionEntity>(
                    LocalDateTime.of(2024, 3, 31, 22, 29),
                    LocalDateTime.of(2024, 3, 31, 22, 31)
                )
                .first()
        }

        assertNotNull(retrieved)
        assertEquals(10L, retrieved.powerAmountConsumed)
    }
}
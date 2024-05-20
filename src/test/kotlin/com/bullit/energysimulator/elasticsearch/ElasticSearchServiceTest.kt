package com.bullit.energysimulator.elasticsearch

import com.bullit.energysimulator.*
import kotlinx.coroutines.flow.first
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
            10.0,
            Rate.T1,
            10.0
        )

        val persisted = runBlocking {
            elasticsearchService.saveConsumption(consumption)
        }

        assertTrue(persisted.isRight())
        assertEquals(10.0, persisted.getOrNull()?.amountConsumed)
    }

    @Order(2)
    @Test
    fun `should persist gas consumption`() {
        val consumption = ElasticGasConsumptionEntity(
            LocalDateTime.of(2024, 3, 31, 22, 30),
            10.0,
            10.0
        )

        val persisted = runBlocking {
            elasticsearchService.saveConsumption(consumption)
        }

        assertTrue(persisted.isRight())
        assertEquals(10.0, persisted.getOrNull()?.amountConsumed)
    }

    @Order(3)
    @Test
    fun `should fetch power consumption`() {
        /*
        We need to refresh the index before doing the search. Elastic refreshes the index
        asynchronously so if you run this test directly after the write operation chances
        are you will not get a hit. By manually refreshing the index you force this process
        and now you are ensured of getting your search results.
         */
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
        assertEquals(10.0, retrieved.amountConsumed)
    }

    @Order(4)
    @Test
    fun `should fetch gas consumption`() {
        /*
        We need to refresh the index before doing the search. Elastic refreshes the index
        asynchronously so if you run this test directly after the write operation chances
        are you will not get a hit. By manually refreshing the index you force this process
        and now you are ensured of getting your search results.
         */
        runBlocking {
            ops
                .indexOps(ElasticGasConsumptionEntity::class.java)
                .refresh()
                .awaitFirstOrNull()
        }

        val retrieved = runBlocking {
            elasticsearchService
                .searchByDateRange<ElasticGasConsumptionEntity>(
                    LocalDateTime.of(2024, 3, 31, 22, 29),
                    LocalDateTime.of(2024, 3, 31, 22, 31)
                )
                .first()
        }

        assertNotNull(retrieved)
        assertEquals(10.0, retrieved.amountConsumed)
    }
}
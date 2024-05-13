package com.bullit.energysimulator.elasticsearch

import com.bullit.energysimulator.ElasticGasConsumptionEntity
import com.bullit.energysimulator.ElasticPowerConsumptionEntity
import com.bullit.energysimulator.logger
import jakarta.annotation.PostConstruct
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations


class ElasticIndices(
    private val ops: ReactiveElasticsearchOperations
) {
    companion object {
        private val logger = logger()
    }

    @PostConstruct
    fun initializeIndices() {
        logger.info("Creating Elastic indices")
        ops
            .indexOps(ElasticPowerConsumptionEntity::class.java)
            .exists()
            .map { exists ->
                if (!exists) {
                    ops
                        .indexOps(ElasticPowerConsumptionEntity::class.java)
                        .create()
                        .then(
                            ops
                                .indexOps(ElasticPowerConsumptionEntity::class.java)
                                .putMapping(ElasticPowerConsumptionEntity::class.java)
                        )
                }
            }
            .then(
                ops
                    .indexOps(ElasticGasConsumptionEntity::class.java)
                    .exists()
                    .map { exists ->
                        if (!exists) {
                            ops
                                .indexOps(ElasticGasConsumptionEntity::class.java)
                                .create()
                                .then(
                                    ops
                                        .indexOps(ElasticGasConsumptionEntity::class.java)
                                        .putMapping(ElasticGasConsumptionEntity::class.java)
                                )
                        }
                    }
            )
            .block()

        logger.info("Created Elastic indices")
    }
}
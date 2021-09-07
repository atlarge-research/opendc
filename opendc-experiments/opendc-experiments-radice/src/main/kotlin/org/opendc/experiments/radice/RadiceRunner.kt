/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.experiments.radice

import mu.KotlinLogging
import org.opendc.compute.workload.*
import org.opendc.compute.workload.topology.apply
import org.opendc.compute.workload.util.VmInterferenceModelReader
import org.opendc.experiments.radice.cost.ec2CostModel
import org.opendc.experiments.radice.cost.latencyCostModel
import org.opendc.experiments.radice.cost.qosCostModel
import org.opendc.experiments.radice.export.InterferenceData
import org.opendc.experiments.radice.export.RiskData
import org.opendc.experiments.radice.export.parquet.ParquetInterferenceDataWriter
import org.opendc.experiments.radice.export.parquet.ParquetRiskDataWriter
import org.opendc.experiments.radice.scenario.ScenarioSpec
import org.opendc.experiments.radice.scenario.WorkloadSpec
import org.opendc.experiments.radice.scenario.topology.toTopology
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.compute.ComputeMetricAggregator
import org.opendc.telemetry.compute.ComputeMonitor
import org.opendc.telemetry.compute.table.ServerTableReader
import org.opendc.telemetry.compute.table.ServiceTableReader
import org.opendc.telemetry.risk.*
import org.opendc.telemetry.risk.cost.constantCostModel
import org.opendc.telemetry.risk.cost.linearCostModel
import org.opendc.telemetry.risk.indicator.*
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.system.measureTimeMillis

/**
 * Helper class for running Risk Analysis experiments.
 */
class RadiceRunner(private val tracePath: File, private val outputPath: File?) {
    /**
     * The logger for this portfolio instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The [ComputeWorkloadLoader] to use for loading the traces.
     */
    private val workloadLoader = ComputeWorkloadLoader(tracePath)

    /**
     * The [VmInterferenceModel] that have been loaded into memory.
     */
    private val interferenceModels = ConcurrentHashMap<WorkloadSpec, VmInterferenceModel>()

    /**
     * Helper class to load the [VmInterferenceModel]s into memory.
     */
    private val interferenceReader = VmInterferenceModelReader()

    /**
     * Run a single [scenario] with the specified seed.
     */
    fun runScenario(scenario: ScenarioSpec, seed: Long): Map<RiskFactor, Double> {
        val random = Random(seed)
        val costs = HashMap<RiskFactor, Double>()

        // Load the interference model into memory if one exists
        val interferenceModelPath = tracePath.resolve(scenario.workload.name).resolve("interference-model.json")
        val interferenceModel = if (scenario.phenomena.hasInterference) {
            // Interference is enabled, but the model might not actually exist for the workload
            // Warn the user if that happens
            if (!interferenceModelPath.exists()) {
                logger.warn { "Interference is enabled for experiment but model not found [$interferenceModelPath]" }
                null
            } else {
                interferenceModels.computeIfAbsent(scenario.workload) { interferenceReader.read(interferenceModelPath) }
            }
        } else {
            null
        }

        val partitions = scenario.partitions + ("seed" to seed.toString())
        val partition = partitions.map { (k, v) -> "$k=$v" }.joinToString("/")

        runBlockingSimulation {
            val workload = scenario.workload
            val runner = ComputeServiceHelper(
                coroutineContext,
                clock,
                scenario.scheduler(random),
                scenario.phenomena.failureInterval?.let { grid5000(it) },
                interferenceModel?.withSeed(random.nextLong())
            )

            val outputPath = outputPath
            val riskWriter = if (outputPath != null) {
                val partitionDir = File(outputPath, "risk/$partition")
                partitionDir.mkdirs() // Create all necessary directories
                ParquetRiskDataWriter(File(partitionDir, "data.parquet"))
            } else {
                null
            }

            val factors = createRiskFactors(scenario)
            factors.associateWithTo(costs) { 0.0 }

            val rm = RiskMonitor(coroutineContext, clock, runner.producers, factors)
            rm.addListener(object : RiskMonitorListener {
                override fun onEvaluation(factor: RiskFactor, point: RiskIndicatorPoint, isViolation: Boolean) {
                    if (!isViolation) {
                        return
                    }

                    val cost = factor.cost.getCost(factor, point)

                    logger.debug { "[${factor.id}] Violation: $point" }

                    riskWriter?.write(RiskData(factor, point, cost))
                    costs.merge(factor, cost, Double::plus)
                }
            })

            val duration = measureTimeMillis {
                try {
                    rm.start()

                    runner.apply(scenario.topology.toTopology(random), optimize = true)
                    runner.run(workload.source.resolve(workloadLoader, random), random.nextLong())
                } finally {
                    runner.close()
                    rm.close()
                    riskWriter?.close()
                }
            }

            logger.info { "Scenario run finished (took ${Duration.ofMillis(duration)})" }

            val interferenceWriter = if (scenario.recordInterference) {
                val partitionDir = File(outputPath, "interference/$partition")
                partitionDir.mkdirs() // Create all necessary directories
                ParquetInterferenceDataWriter(File(partitionDir, "data.parquet"))
            } else {
                null
            }
            val agg = ComputeMetricAggregator()
            for (producer in runner.producers) {
                agg.process(producer.collectAllMetrics())
            }
            agg.collect(object : ComputeMonitor {
                override fun record(reader: ServerTableReader) {
                    interferenceWriter?.write(InterferenceData(reader.server, reader.cpuStealTime + reader.cpuLostTime))
                }

                override fun record(reader: ServiceTableReader) {
                    if (reader.serversActive > 0) {
                        logger.warn {
                            "Simulation was incomplete: " +
                                "Success=${reader.attemptsSuccess} " +
                                "Failure=${reader.attemptsFailure} " +
                                "Error=${reader.attemptsError} " +
                                "Pending=${reader.serversPending} " +
                                "Active=${reader.serversActive}"
                        }
                    }
                }
            })
            interferenceWriter?.close()
        }

        return costs
    }

    private fun createRiskFactors(scenario: ScenarioSpec): List<RiskFactor> {
        return listOf(
            // Customer
            RiskFactor(
                "customer:availability",
                indicator = serverAvailability(),
                objective = RiskObjective(min = scenario.availabilitySla.target),
                cost = ec2CostModel(scenario.costs.costPerCpuH, scenario.availabilitySla.refunds),
                period = monthPeriod()
            ),
            RiskFactor(
                "customer:cpu_interference",
                indicator = cpuInterference(),
                objective = RiskObjective(max = scenario.qosSla.target),
                cost = qosCostModel(scenario.costs.costPerCpuH, scenario.qosSla.refunds),
                period = dayPeriod()
            ),
            RiskFactor(
                "customer:latency",
                indicator = schedulingLatency(),
                objective = RiskObjective(max = Duration.ofHours(1).toMillis().toDouble()),
                cost = latencyCostModel(scenario.costs.costPerCpuH),
                period = monthPeriod()
            ),
            // Company
            RiskFactor(
                "company:power",
                powerConsumption(scenario.pue),
                objective = RiskObjective(max = 0.0),
                cost = linearCostModel(scenario.costs.powerCost / 1000),
                period = monthPeriod()
            ),
            RiskFactor(
                "company:co2",
                indicator = co2Emissions(scenario.pue, scenario.carbonFootprint),
                objective = RiskObjective(max = 0.0),
                cost = linearCostModel(scenario.costs.carbonCost / 1000),
                period = monthPeriod(),
            ),
            RiskFactor(
                "company:host_saturation",
                indicator = hostCpuSaturation(percentile = 0.95),
                objective = RiskObjective(max = 0.75),
                cost = constantCostModel(scenario.costs.investigationCost),
                period = dayPeriod()
            ),
            RiskFactor(
                "company:host_imbalance",
                indicator = hostCpuImbalance(),
                objective = RiskObjective(max = 0.2),
                cost = constantCostModel(scenario.costs.investigationCost),
                period = dayPeriod()
            ),
            // Social
            RiskFactor(
                "society:co2",
                indicator = co2Emissions(scenario.pue, scenario.carbonFootprint),
                objective = RiskObjective(max = 0.0),
                cost = linearCostModel(scenario.costs.carbonSocialCost / 1000),
                period = monthPeriod()
            ),
        )
    }
}

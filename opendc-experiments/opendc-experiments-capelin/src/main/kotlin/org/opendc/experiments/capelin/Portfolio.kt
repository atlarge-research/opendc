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

package org.opendc.experiments.capelin

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.*
import org.opendc.compute.api.Server
import org.opendc.compute.workload.ComputeServiceHelper
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.workload.createComputeScheduler
import org.opendc.compute.workload.export.parquet.ParquetComputeMonitor
import org.opendc.compute.workload.grid5000
import org.opendc.compute.workload.telemetry.ComputeMetricReader
import org.opendc.compute.workload.topology.apply
import org.opendc.experiments.capelin.model.OperationalPhenomena
import org.opendc.experiments.capelin.model.Topology
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.topology.clusterTopology
import org.opendc.harness.dsl.Experiment
import org.opendc.harness.dsl.anyOf
import org.opendc.simulator.core.runBlockingSimulation
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.math.roundToLong

/**
 * A portfolio represents a collection of scenarios are tested for the work.
 *
 * @param name The name of the portfolio.
 */
abstract class Portfolio(name: String) : Experiment(name) {
    /**
     * The configuration to use.
     */
    private val config = ConfigFactory.load().getConfig("opendc.experiments.capelin")

    /**
     * The path to the original VM placements file.
     */
    private val vmPlacements by anyOf(emptyMap<String, String>())

    /**
     * The topology to test.
     */
    abstract val topology: Topology

    /**
     * The workload to test.
     */
    abstract val workload: Workload

    /**
     * The operational phenomenas to consider.
     */
    abstract val operationalPhenomena: OperationalPhenomena

    /**
     * The allocation policies to consider.
     */
    abstract val allocationPolicy: String

    /**
     * A helper class to load workload traces.
     */
    private val workloadLoader = ComputeWorkloadLoader(File(config.getString("trace-path")))

    /**
     * Perform a single trial for this portfolio.
     */
    override fun doRun(repeat: Int): Unit = runBlockingSimulation {
        val seeder = Random(repeat.toLong())

        val computeScheduler = createComputeScheduler(allocationPolicy, seeder, vmPlacements)
        val failureModel =
            if (operationalPhenomena.failureFrequency > 0)
                grid5000(Duration.ofSeconds((operationalPhenomena.failureFrequency * 60).roundToLong()))
            else
                null
        val (vms, interferenceModel) = workload.source.resolve(workloadLoader, seeder)
        val runner = ComputeServiceHelper(
            coroutineContext,
            clock,
            computeScheduler,
            failureModel,
            interferenceModel?.withSeed(repeat.toLong())
        )

        val topology = clusterTopology(File(config.getString("env-path"), "${topology.name}.txt"))
        val servers = mutableListOf<Server>()
        val exporter = ComputeMetricReader(
            this,
            clock,
            runner.service,
            servers,
            ParquetComputeMonitor(
                File(config.getString("output-path")),
                "portfolio_id=$name/scenario_id=$id/run_id=$repeat",
                bufferSize = 4096
            ),
            exportInterval = Duration.ofMinutes(5)
        )

        try {
            // Instantiate the desired topology
            runner.apply(topology)

            coroutineScope {
                // Run the workload trace
                runner.run(vms, seeder.nextLong(), servers)

                // Stop the metric collection
                exporter.close()
            }
        } finally {
            runner.close()
            exporter.close()
        }
    }
}

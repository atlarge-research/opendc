/*
 * Copyright (c) 2022 AtLarge Research
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

import org.opendc.compute.service.ComputeService
import org.opendc.experiments.capelin.model.Scenario
import org.opendc.experiments.capelin.topology.clusterTopology
import org.opendc.experiments.compute.ComputeWorkloadLoader
import org.opendc.experiments.compute.createComputeScheduler
import org.opendc.experiments.compute.export.parquet.ParquetComputeMonitor
import org.opendc.experiments.compute.grid5000
import org.opendc.experiments.compute.registerComputeMonitor
import org.opendc.experiments.compute.replay
import org.opendc.experiments.compute.setupComputeService
import org.opendc.experiments.compute.setupHosts
import org.opendc.experiments.provisioner.Provisioner
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.time.Duration
import java.util.Random
import kotlin.math.roundToLong

/**
 * Helper class for running the Capelin experiments.
 *
 * @param envPath The path to the directory containing the environments.
 * @param tracePath The path to the directory containing the traces.
 * @param outputPath The path to the directory where the output should be written (or `null` if no output should be generated).
 */
public class CapelinRunner(
    private val envPath: File,
    tracePath: File,
    private val outputPath: File?
) {
    /**
     * The [ComputeWorkloadLoader] to use for loading the traces.
     */
    private val workloadLoader = ComputeWorkloadLoader(tracePath)

    /**
     * Run a single [scenario] with the specified seed.
     */
    fun runScenario(scenario: Scenario, seed: Long) = runSimulation {
        val serviceDomain = "compute.opendc.org"
        val topology = clusterTopology(File(envPath, "${scenario.topology.name}.txt"))

        Provisioner(dispatcher, seed).use { provisioner ->
            provisioner.runSteps(
                setupComputeService(serviceDomain, { createComputeScheduler(scenario.allocationPolicy, Random(it.seeder.nextLong())) }),
                setupHosts(serviceDomain, topology, optimize = true)
            )

            if (outputPath != null) {
                val partitions = scenario.partitions + ("seed" to seed.toString())
                val partition = partitions.map { (k, v) -> "$k=$v" }.joinToString("/")

                provisioner.runStep(
                    registerComputeMonitor(
                        serviceDomain,
                        ParquetComputeMonitor(
                            outputPath,
                            partition,
                            bufferSize = 4096
                        )
                    )
                )
            }

            val service = provisioner.registry.resolve(serviceDomain, ComputeService::class.java)!!
            val vms = scenario.workload.source.resolve(workloadLoader, Random(seed))
            val operationalPhenomena = scenario.operationalPhenomena
            val failureModel =
                if (operationalPhenomena.failureFrequency > 0) {
                    grid5000(Duration.ofSeconds((operationalPhenomena.failureFrequency * 60).roundToLong()))
                } else {
                    null
                }

            service.replay(timeSource, vms, seed, failureModel = failureModel, interference = operationalPhenomena.hasInterference)
        }
    }
}

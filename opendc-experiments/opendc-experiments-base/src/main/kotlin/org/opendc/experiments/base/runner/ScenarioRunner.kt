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

package org.opendc.experiments.base.runner

import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.ComputeSchedulerEnum
import org.opendc.compute.service.scheduler.createComputeScheduler
import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.compute.telemetry.export.parquet.ParquetComputeMonitor
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.experiments.base.models.portfolio.Portfolio
import org.opendc.experiments.base.models.scenario.PowerModelSpec
import org.opendc.experiments.base.models.scenario.Scenario
import org.opendc.experiments.base.models.scenario.getWorkloadType
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.time.Duration
import java.util.Random
import java.util.concurrent.ForkJoinPool
import java.util.stream.LongStream

public fun runPortfolio(
    portfolio: Portfolio,
    parallelism: Int,
) {
    val pool = ForkJoinPool(parallelism)

    for (scenario in portfolio.scenarios) {
        runScenario(scenario[0], pool)
    }
}

/**
 * Run scenario when no pool is available for parallel execution
 *
 * @param scenario The scenario to run
 * @param parallelism The number of scenarios that can be run in parallel
 */
public fun runScenario(
    scenarios: List<Scenario>,
    parallelism: Int,
) {
    for (scenario in scenarios) {
        val pool = ForkJoinPool(parallelism)
        println("\n\n" +
        """
            ================================================================================
                      Running scenario: ${scenario.name}
            ================================================================================
        """.trimIndent()
        )
        runScenario(
            scenario,
            pool
        )
    }
}

/**
 * Run scenario when a pool is available for parallel execution
 * The scenario is run multiple times based on the user input
 *
 * @param scenario The scenario to run
 * @param pool The pool on which to run the scenarios
 */
public fun runScenario(
    scenario: Scenario,
    pool: ForkJoinPool,
) {
    val pb = ProgressBarBuilder()
        .setInitialMax(scenario.runs.toLong())
        .setStyle(ProgressBarStyle.ASCII)
        .setTaskName("Simulating...")
        .build()

    pool.submit {
        LongStream.range(0, scenario.runs.toLong())
            .parallel()
            .forEach {
                runScenario(scenario, scenario.initialSeed + it)
                pb.step()
            }
        pb.close()
    }.join()
}

/**
 * Run a single scenario with a specific seed
 *
 * @param scenario The scenario to run
 * @param seed The starting seed of the random generator.
 */
public fun runScenario(
    scenario: Scenario,
    seed: Long,
): Unit =
    runSimulation {
        val serviceDomain = "compute.opendc.org"
        Provisioner(dispatcher, seed).use { provisioner ->
            provisioner.runSteps(
                setupComputeService(
                    serviceDomain,
                    { createComputeScheduler(ComputeSchedulerEnum.Mem, Random(it.seeder.nextLong())) }),
                setupHosts(serviceDomain, scenario.topology, optimize = true),
            )

            val partition = scenario.name + "/seed=$seed"

            val workloadLoader = ComputeWorkloadLoader(File(scenario.workload.pathToFile))
            val vms = getWorkloadType(scenario.workload.type).resolve(workloadLoader, Random(seed))

            val startTime = Duration.ofMillis(vms.minOf { it.startTime }.toEpochMilli())

            // saves in a seed folder
//            provisioner.runStep(
//                registerComputeMonitor(
//                    serviceDomain,
//                    ParquetComputeMonitor(
//                        File(scenario.outputFolder),
//                        partition,
//                        bufferSize = 4096,
//                    ),
//                    Duration.ofSeconds(scenario.exportModel.exportInterval),
//                    startTime,
//                ),
//            )

            // saves results in an output folder
            // val outputFolderPath = "output/simulation-results/"
            // if (File(outputFolderPath).exists()) File(outputFolderPath).deleteRecursively()
            provisioner.runStep(
                registerComputeMonitor(
                    serviceDomain,
                    ParquetComputeMonitor(
                        File("output/simulation-results/"),
                        scenario.name,
                        bufferSize = 4096,
                        ),
                    Duration.ofSeconds(scenario.exportModel.exportInterval),
                    startTime,
                ),
            )

            val service = provisioner.registry.resolve(serviceDomain, ComputeService::class.java)!!
            service.replay(timeSource, vms, seed, failureModel = scenario.failureModel)
        }
    }

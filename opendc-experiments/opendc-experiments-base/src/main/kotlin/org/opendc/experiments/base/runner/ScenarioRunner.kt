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

import getWorkloadType
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.opendc.compute.carbon.CarbonTrace
import org.opendc.compute.carbon.getCarbonTrace
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.ComputeSchedulerEnum
import org.opendc.compute.service.scheduler.createComputeScheduler
import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.compute.telemetry.export.parquet.ParquetComputeMonitor
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.experiments.base.models.scenario.Scenario
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.time.Duration
import java.util.Random
import java.util.concurrent.ForkJoinPool
import java.util.stream.LongStream

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
    val ansiReset = "\u001B[0m"
    val ansiGreen = "\u001B[32m"
    val ansiBlue = "\u001B[34m"
    clearOutputFolder(scenarios[0].outputFolder)

    for (scenario in scenarios) {
        val pool = ForkJoinPool(parallelism)
        println(
            "\n\n$ansiGreen================================================================================$ansiReset",
        )
        println("$ansiBlue Running scenario: ${scenario.name} $ansiReset")
        println("$ansiGreen================================================================================$ansiReset")
        runScenario(
            scenario,
            pool,
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
    val pb =
        ProgressBarBuilder().setInitialMax(scenario.runs.toLong()).setStyle(ProgressBarStyle.ASCII)
            .setTaskName("Simulating...").build()

    pool.submit {
        LongStream.range(0, scenario.runs.toLong()).parallel().forEach {
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
                    { createComputeScheduler(ComputeSchedulerEnum.Mem, Random(it.seeder.nextLong())) },
                ),
                setupHosts(serviceDomain, scenario.topology, optimize = true),
            )

            val workloadLoader = ComputeWorkloadLoader(File(scenario.workload.pathToFile))
            val vms = getWorkloadType(scenario.workload.type).resolve(workloadLoader, Random(seed))

            val carbonTrace = getCarbonTrace(scenario.carbonTracePath)
            val startTime = Duration.ofMillis(vms.minOf { it.startTime }.toEpochMilli())
            saveInOutputFolder(provisioner, serviceDomain, scenario, seed, startTime, carbonTrace)

            val service = provisioner.registry.resolve(serviceDomain, ComputeService::class.java)!!
            service.replay(timeSource, vms, seed, failureModel = scenario.failureModel)
        }
    }

/**
 * When the simulation is run, saves the simulation results into a seed folder. This is useful for debugging purposes.
 * @param provisioner The provisioner used to setup and run the simulation.
 * @param serviceDomain The domain of the compute service.
 * @param scenario The scenario being run in the simulation.
 * @param seed The seed used for randomness in the simulation.
 * @param partition The partition name for the output data.
 * @param startTime The start time of the simulation.

 */
public fun saveInSeedFolder(
    provisioner: Provisioner,
    serviceDomain: String,
    scenario: Scenario,
    seed: Long,
    partition: String,
    startTime: Duration,
) {
    provisioner.runStep(
        registerComputeMonitor(
            serviceDomain,
            ParquetComputeMonitor(
                File(scenario.outputFolder),
                partition,
                bufferSize = 4096,
            ),
            Duration.ofSeconds(scenario.exportModel.exportInterval),
            startTime,
        ),
    )
}

/**
 * Saves the simulation results into a specific output folder received from the input.
 *
 * @param provisioner The provisioner used to setup and run the simulation.
 * @param serviceDomain The domain of the compute service.
 * @param scenario The scenario being run.
 * @param seed The seed of the current run
 * @param startTime The start time of the simulation given by the workload trace.
 * @param carbonTrace The carbon trace used to determine carbon emissions.
 */
public fun saveInOutputFolder(
    provisioner: Provisioner,
    serviceDomain: String,
    scenario: Scenario,
    seed: Long,
    startTime: Duration,
    carbonTrace: CarbonTrace,
) {
    provisioner.runStep(
        registerComputeMonitor(
            serviceDomain,
            ParquetComputeMonitor(
                File("${scenario.outputFolder}/${scenario.name}"),
                "seed=$seed",
                bufferSize = 4096,
            ),
            Duration.ofSeconds(scenario.exportModel.exportInterval),
            startTime,
            carbonTrace,
        ),
    )
}

/**
 * Utility function, in case we want to delete the previous simulation results.
 * @param outputFolderPath The output folder to remove
 */
public fun clearOutputFolder(outputFolderPath: String) {
    if (File(outputFolderPath).exists()) File(outputFolderPath).deleteRecursively()
}

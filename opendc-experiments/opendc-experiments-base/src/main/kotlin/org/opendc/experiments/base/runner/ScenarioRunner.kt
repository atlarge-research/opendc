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

@file:Suppress("DEPRECATION")

package org.opendc.experiments.base.runner

import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.telemetry.parquet.ComputeExportConfig
import org.opendc.compute.simulator.telemetry.parquet.ParquetComputeMonitor
import org.opendc.compute.simulator.telemetry.parquet.withGpuColumns
import org.opendc.compute.topology.clusterTopology
import org.opendc.experiments.base.experiment.Scenario
import org.opendc.experiments.base.experiment.specs.allocation.TimeShiftAllocationPolicySpec
import org.opendc.experiments.base.experiment.specs.allocation.createComputeScheduler
import org.opendc.experiments.base.experiment.specs.allocation.createTaskStopper
import org.opendc.experiments.base.experiment.specs.getWorkload
import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.CarbonReceiver
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.time.Duration
import java.util.Random
import java.util.concurrent.ForkJoinPool
import java.util.stream.LongStream

/**
 * Run scenario when a pool is available for parallel execution
 * The scenario is run multiple times based on the user input
 *
 * @param scenario The scenario to run
 */
public fun runScenario(
    scenario: Scenario,
    strictReader: Boolean = false,
) {
    val pb =
        ProgressBarBuilder().setInitialMax(scenario.runs.toLong()).setStyle(ProgressBarStyle.ASCII)
            .setTaskName("Simulating...").build()

    val pool = ForkJoinPool(5)
    pool.submit {
        LongStream.range(0, scenario.runs.toLong()).parallel().forEach {
            runScenario(scenario, scenario.initialSeed + it, strictReader)
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
    strictReader: Boolean = false,
): Unit =
    runSimulation {
        val serviceDomain = "compute.opendc.org"
        Provisioner(dispatcher, seed).use { provisioner ->

            val workload =
                getWorkload(
                    scenario.workloadSpec,
                    scenario.checkpointModelSpec,
                )

            val startTimeLong = workload.minOf { it.submittedAt }
            val startTime = Duration.ofMillis(startTimeLong)

            val topology = clusterTopology(scenario.topologyPathSpec.pathToFile, strictReader)
            val numHosts = topology.sumOf { it.hostSpecs.size }
            val gpuCount = topology.flatMap { it.hostSpecs }.maxOfOrNull { it.model.gpuModels.size } ?: 0

            provisioner.runSteps(
                setupComputeService(
                    serviceDomain,
                    {
                        val computeScheduler =
                            createComputeScheduler(
                                scenario.allocationPolicySpec,
                                Random(it.seeder.nextLong()),
                                timeSource,
                                numHosts,
                            )

                        provisioner.registry.register(serviceDomain, ComputeScheduler::class.java, computeScheduler)

                        return@setupComputeService computeScheduler
                    },
                    maxNumFailures = scenario.maxNumFailures,
                ),
                setupHosts(serviceDomain, topology, startTimeLong),
            )

            initializeExportModel(
                provisioner,
                serviceDomain,
                scenario,
                seed,
                startTime,
                scenario.id,
                computeExportConfig =
                    scenario.exportModelSpec.computeExportConfig.withGpuColumns(gpuCount),
            )

            val service = provisioner.registry.resolve(serviceDomain, ComputeService::class.java)!!
            service.setTasksExpected(workload.size)
            service.addMetricReader(provisioner.getMonitors())

            // TODO: Improve how the allocation policy and carbon model are created.
            var carbonModel: CarbonModel?
            if (provisioner.registry.hasService(serviceDomain, CarbonModel::class.java)) {
                carbonModel = provisioner.registry.resolve(serviceDomain, CarbonModel::class.java)!!

                val computeScheduler = provisioner.registry.resolve(serviceDomain, ComputeScheduler::class.java)!!
                if (computeScheduler is CarbonReceiver) {
                    carbonModel.addReceiver(computeScheduler)
                    carbonModel.addReceiver(service)
                }

                if (scenario.allocationPolicySpec is TimeShiftAllocationPolicySpec) {
                    val taskStopper =
                        createTaskStopper(
                            scenario.allocationPolicySpec.taskStopper,
                            coroutineContext,
                            timeSource,
                        )
                    if (taskStopper != null) {
                        taskStopper.setService(service)
                        carbonModel.addReceiver(taskStopper)
                    }
                }
            }

            // Starts replaying the given scenario
            service.replay(
                timeSource,
                workload,
                failureModelSpec = scenario.failureModelSpec,
                seed = seed,
            )
        }
    }

/**
 * Saves the simulation results into a specific output folder received from the input.A
 *
 * @param provisioner The provisioner used to setup and run the simulation.
 * @param serviceDomain The domain of the compute service.
 * @param scenario The scenario being run.
 * @param seed The seed of the current run
 * @param startTime The start time of the simulation given by the workload trace.
 */
public fun initializeExportModel(
    provisioner: Provisioner,
    serviceDomain: String,
    scenario: Scenario,
    seed: Long,
    startTime: Duration,
    index: Int,
    computeExportConfig: ComputeExportConfig = scenario.exportModelSpec.computeExportConfig,
) {
    provisioner.runStep(
        registerComputeMonitor(
            serviceDomain,
            ParquetComputeMonitor(
                File("${scenario.outputFolder}/raw-output/$index"),
                "seed=$seed",
                bufferSize = 4096,
                scenario.exportModelSpec.filesToExportDict,
                computeExportConfig = computeExportConfig,
            ),
            Duration.ofSeconds(scenario.exportModelSpec.exportInterval),
            startTime,
            scenario.exportModelSpec.filesToExportDict,
            scenario.exportModelSpec.printFrequency,
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

/**
 * Utility function to create the output folder structure for the simulation results.
 * @param folderPath The path to the output folder
 */
public fun setupOutputFolderStructure(folderPath: String) {
    val trackrPath = "$folderPath/trackr.json"
    val simulationAnalysisPath = "$folderPath/simulation-analysis/"
    val energyAnalysisPath = "$simulationAnalysisPath/power_draw/"
    val emissionsAnalysisPath = "$simulationAnalysisPath/carbon_emission/"

    File(folderPath).mkdir()
    File(trackrPath).createNewFile()
    File(simulationAnalysisPath).mkdir()
    File(energyAnalysisPath).mkdir()
    File(emissionsAnalysisPath).mkdir()
}

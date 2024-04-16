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

package org.opendc.experiments.metamodel

import org.opendc.compute.service.ComputeService
import org.opendc.experiments.metamodel.topology.clusterTopology
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.experiments.metamodel.portfolio.MetamodelPortfolio
import org.opendc.experiments.metamodel.portfolio.readCsvIntoArray
import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.simulator.compute.power.CpuPowerModel
import org.opendc.simulator.compute.power.CpuPowerModels
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Random
import kotlin.math.roundToLong
import org.opendc.compute.service.scheduler.createComputeScheduler
import org.opendc.compute.simulator.failure.grid5000
import org.opendc.compute.simulator.provisioner.ProvisioningStep
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.telemetry.export.parquet.ParquetComputeMonitor
import org.opendc.compute.topology.HostSpec
import org.opendc.experiments.base.portfolio.model.Scenario
import org.opendc.experiments.base.runner.replay

/**
 * Helper class for running the Metamodel experiments.
 *
 * @param envPath The path to the directory containing the environments.
 * @param tracePath The path to the directory containing the traces.
 * @param outputPath The path to the directory where the output should be written (or `null` if no output should be generated).
 */
public class MetamodelRunner(
    private val envPath: File,
    tracePath: File,
    private val outputPath: File?
) {
    /**
     * The [ComputeWorkloadLoader] to use for loading the traces.
     */
    private val workloadLoader = ComputeWorkloadLoader(tracePath)
    private val SERVICE_DOMAIN = "compute.opendc.org"
    private val _outputFolderName: String = getOutputFolderName()

    /**
     * Run a single [scenario] with the specified seed.
     */
    fun runScenario(scenario: Scenario, seed: Long) = runSimulation {
        val childName = "${scenario.topology.name}.txt" // name of the file with experiment config
        val fileTopology = File(
            envPath,
            childName
        ) // from name to path - for now, just single.txt and multi.txt, with configuration in the file

        /*
        Here, we ===CONFIGURE THE TOPOLOGY===, we are basically setting up the experiment in this case, our topology
        has (by default) 8 CPUs, with frequency 3200, one memory with 128000 size and no storage/net (is net = network?)
        all of these, we take from the configuration file, aforementioned
         */
        val topology = clusterTopology(
            fileTopology,
            getCpuPowerModel(scenario.energyModel, 350.0, 200.0)
        )
        val allocationPolicy = scenario.allocationPolicy // the policy used to allocate resources e.g., active-servers

        Provisioner(dispatcher, seed).use { provisioner ->
            val experimentSetup = setupExperiment(allocationPolicy, topology)
            val computeService = experimentSetup.first
            val hosts = experimentSetup.second

            // this is the place where we run the steps to configure the experiment
            provisioner.runSteps(computeService, hosts)

            // configuring the model
            if (outputPath != null) {
                // we append to the partition map, we set the topology, the workload, and the seed
                val partitions =
                    scenario.partitions + ("seed" to seed.toString()) + ("simulation" to _outputFolderName)

                // we create a path for the partition (e.g., "topology=single/workload=bitbrains-small/seed=0/simulation=...")
                // the following 2 lines skip the first element form partitions when reading
                val filteredPartitions = partitions.entries.drop(1).associate { it.toPair() }
                val partition = filteredPartitions.map { (k, v) -> "$k=$v" }.joinToString("/")

                val parquetComputeMonitor = ParquetComputeMonitor(
                    outputPath,
                    partition,
                    bufferSize = 4096,
                    outputName = "${scenario.topology.name}-${scenario.energyModel}-${scenario.allocationPolicy}"
                )

                val registerComputeMonitor = registerComputeMonitor(
                    SERVICE_DOMAIN,
                    parquetComputeMonitor
                )
                provisioner.runStep(registerComputeMonitor)
            }

            val service = provisioner.registry.resolve(SERVICE_DOMAIN, ComputeService::class.java)!!

            // we are still configuring here
            val vms = scenario.workload.source.resolve(workloadLoader, Random(seed))
            val operationalPhenomena = scenario.operationalPhenomena
            val failureModel =
                if (operationalPhenomena.failureFrequency > 0) {
                    grid5000(Duration.ofSeconds((operationalPhenomena.failureFrequency * 60).roundToLong()))
                } else {
                    null
                }


            /*
            the following chunk of code was written for debugging purposes and understanding the code
             */

            // up until now, all we did was configuration, now we are actually running the experiment
            service.replay( // replay launches the servers
                timeSource, // used to align the running jobs~, we use this to make sure everything stars at the same time, and we can compare (we use the same clock - like the CPU)
                vms, // all the jobs that need to be run
                seed, // a seed
                failureModel = failureModel, //
                interference = operationalPhenomena.hasInterference
            )
        }
    }

    fun setupExperiment(allocationPolicy: String, topology: List<HostSpec>): Pair<ProvisioningStep, ProvisioningStep> {
        val computeService = setupComputeService(
            SERVICE_DOMAIN,
            {
                createComputeScheduler( // this function configures the scheduler,
                    allocationPolicy, //taking the allocation policy as filters,
                    Random(it.seeder.nextLong()) // and the random as weighers
                    // we do not give any placement policy, so it is empty (placement = where to put the VMs?)
                )
            }
        )
        println("Compute Service: $computeService")

        val hosts = setupHosts(SERVICE_DOMAIN, topology, optimize = true)
        return Pair(computeService, hosts)
    }


    // DO NOT REMOVE
    private fun getOutputFolderName(): String {
        // gets the last column
        val folderName =
            MetamodelPortfolio().inputFile[1][readCsvIntoArray(fileName = "input/configuration-input.csv")[1].size - 1]

        if (folderName == "") {
            return (LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
                .toString())
        }

        return folderName
    }

    private fun getCpuPowerModel(model: String, maxPower: Double, idlePower: Double): CpuPowerModel {
        return when (model) {
//            "constant" -> CpuPowerModels.constant(maxPower)
            "sqrt" -> CpuPowerModels.sqrt(maxPower, idlePower)
            "linear" -> CpuPowerModels.linear(maxPower, idlePower)
            "square" -> CpuPowerModels.square(maxPower, idlePower)
            "cubic" -> CpuPowerModels.cubic(maxPower, idlePower)
            // "mse" -> CpuPowerModels.mse(maxPower, idlePower, 0.0) TOBEFIXED
            // "asymptotic" -> CpuPowerModels.asymptotic(maxPower, idlePower, 0.0, false), TOBEFIXED
            // "interpolate" -> CpuPowerModels.interpolate(maxPower, idlePower, 0.0, false), TOBEFIXED
            else -> throw IllegalArgumentException("Unknown CPU power model: $model")
        }
    }
}

/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.experiment

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.compute.virt.service.allocation.AvailableCoreMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.AvailableMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.NumberOfActiveServersAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ProvisionedCoresAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.RandomAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ReplayAllocationPolicy
import com.atlarge.opendc.experiments.sc20.experiment.model.CompositeWorkload
import com.atlarge.opendc.experiments.sc20.experiment.monitor.ParquetExperimentMonitor
import com.atlarge.opendc.experiments.sc20.runner.TrialExperimentDescriptor
import com.atlarge.opendc.experiments.sc20.runner.execution.ExperimentExecutionContext
import com.atlarge.opendc.experiments.sc20.trace.Sc20ParquetTraceReader
import com.atlarge.opendc.experiments.sc20.trace.Sc20RawParquetTraceReader
import com.atlarge.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.io.File
import java.util.ServiceLoader
import kotlin.random.Random

/**
 * The logger for the experiment scenario.
 */
private val logger = KotlinLogging.logger {}

/**
 * The provider for the simulation engine to use.
 */
private val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()

/**
 * An experiment run represent a single invocation of a trial and is used to distinguish between repetitions of the
 * same set of parameters.
 */
public data class Run(override val parent: Scenario, val id: Int, val seed: Int) : TrialExperimentDescriptor() {
    override suspend fun invoke(context: ExperimentExecutionContext) {
        val experiment = parent.parent.parent
        val system = provider("experiment-$id")
        val root = system.newDomain("root")
        val seeder = Random(seed)
        val environment = Sc20ClusterEnvironmentReader(File(experiment.environments, "${parent.topology.name}.txt"))

        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = when (parent.allocationPolicy) {
            "mem" -> AvailableMemoryAllocationPolicy()
            "mem-inv" -> AvailableMemoryAllocationPolicy(true)
            "core-mem" -> AvailableCoreMemoryAllocationPolicy()
            "core-mem-inv" -> AvailableCoreMemoryAllocationPolicy(true)
            "active-servers" -> NumberOfActiveServersAllocationPolicy()
            "active-servers-inv" -> NumberOfActiveServersAllocationPolicy(true)
            "provisioned-cores" -> ProvisionedCoresAllocationPolicy()
            "provisioned-cores-inv" -> ProvisionedCoresAllocationPolicy(true)
            "random" -> RandomAllocationPolicy(Random(seeder.nextInt()))
            "replay" -> ReplayAllocationPolicy(experiment.vmPlacements)
            else -> throw IllegalArgumentException("Unknown policy ${parent.allocationPolicy}")
        }

        @Suppress("UNCHECKED_CAST")
        val rawTraceReaders =
            context.cache.computeIfAbsent("raw-trace-readers") { mutableMapOf<String, Sc20RawParquetTraceReader>() } as MutableMap<String, Sc20RawParquetTraceReader>
        val rawReaders = synchronized(rawTraceReaders) {
            val workloadNames = if (parent.workload is CompositeWorkload) {
                parent.workload.workloads.map { it.name }
            } else {
                listOf(parent.workload.name)
            }

            workloadNames.map { workloadName ->
                rawTraceReaders.computeIfAbsent(workloadName) {
                    logger.info { "Loading trace $workloadName" }
                    Sc20RawParquetTraceReader(File(experiment.traces, workloadName))
                }
            }
        }

        val performanceInterferenceModel = experiment.performanceInterferenceModel
            ?.takeIf { parent.operationalPhenomena.hasInterference }
            ?.construct(seeder) ?: emptyMap()
        val trace = Sc20ParquetTraceReader(rawReaders, performanceInterferenceModel, parent.workload, seed)

        val monitor = ParquetExperimentMonitor(this)

        root.launch {
            val (bareMetalProvisioner, scheduler) = createProvisioner(
                root,
                environment,
                allocationPolicy
            )

            val failureDomain = if (parent.operationalPhenomena.failureFrequency > 0) {
                logger.debug("ENABLING failures")
                createFailureDomain(
                    seeder.nextInt(),
                    parent.operationalPhenomena.failureFrequency,
                    bareMetalProvisioner,
                    chan
                )
            } else {
                null
            }

            attachMonitor(scheduler, monitor)
            processTrace(
                trace,
                scheduler,
                chan,
                monitor,
                experiment.vmPlacements
            )

            logger.debug("SUBMIT=${scheduler.submittedVms}")
            logger.debug("FAIL=${scheduler.unscheduledVms}")
            logger.debug("QUEUED=${scheduler.queuedVms}")
            logger.debug("RUNNING=${scheduler.runningVms}")
            logger.debug("FINISHED=${scheduler.finishedVms}")

            failureDomain?.cancel()
            scheduler.terminate()
        }

        try {
            system.run()
        } finally {
            system.terminate()
            monitor.close()
        }
    }
}

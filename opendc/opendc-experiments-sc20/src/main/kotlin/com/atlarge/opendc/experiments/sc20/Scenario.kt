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

package com.atlarge.opendc.experiments.sc20

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.compute.virt.service.allocation.AvailableCoreMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.AvailableMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.NumberOfActiveServersAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ProvisionedCoresAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.RandomAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ReplayAllocationPolicy
import com.atlarge.opendc.experiments.sc20.reporter.ExperimentReporter
import com.atlarge.opendc.format.environment.EnvironmentReader
import com.atlarge.opendc.format.trace.TraceReader
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
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
 * A scenario represents a single point in the design space (a unique combination of parameters).
 */
public class Scenario(
    val portfolio: Portfolio,
    val repetitions: Int,
    val topology: Topology,
    val workload: Workload,
    val allocationPolicy: String,
    val failureFrequency: Double,
    val hasInterference: Boolean
) {
    /**
     * The runs this scenario consists of.
     */
    public val runs: Sequence<Run> = sequence {
        repeat(repetitions) { i ->
            yield(Run(this@Scenario, i, i))
        }
    }

    /**
     * Perform a single run of this scenario.
     */
    public operator fun invoke(run: Run, reporter: ExperimentReporter, environment: EnvironmentReader, trace: TraceReader<VmWorkload>) {
        val system = provider("experiment-${run.id}")
        val root = system.newDomain("root")
        val seeder = Random(run.seed)

        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = when (this.allocationPolicy) {
            "mem" -> AvailableMemoryAllocationPolicy()
            "mem-inv" -> AvailableMemoryAllocationPolicy(true)
            "core-mem" -> AvailableCoreMemoryAllocationPolicy()
            "core-mem-inv" -> AvailableCoreMemoryAllocationPolicy(true)
            "active-servers" -> NumberOfActiveServersAllocationPolicy()
            "active-servers-inv" -> NumberOfActiveServersAllocationPolicy(true)
            "provisioned-cores" -> ProvisionedCoresAllocationPolicy()
            "provisioned-cores-inv" -> ProvisionedCoresAllocationPolicy(true)
            "random" -> RandomAllocationPolicy(Random(seeder.nextInt()))
            "replay" -> ReplayAllocationPolicy(emptyMap())
            else -> throw IllegalArgumentException("Unknown policy ${this.allocationPolicy}")
        }

        root.launch {
            val (bareMetalProvisioner, scheduler) = createProvisioner(root, environment, allocationPolicy)

            val failureDomain = if (failureFrequency > 0) {
                logger.debug("ENABLING failures")
                createFailureDomain(seeder.nextInt(), failureFrequency, bareMetalProvisioner, chan)
            } else {
                null
            }

            attachMonitor(scheduler, reporter)
            processTrace(trace, scheduler, chan, reporter, emptyMap())

            logger.debug("SUBMIT=${scheduler.submittedVms}")
            logger.debug("FAIL=${scheduler.unscheduledVms}")
            logger.debug("QUEUED=${scheduler.queuedVms}")
            logger.debug("RUNNING=${scheduler.runningVms}")
            logger.debug("FINISHED=${scheduler.finishedVms}")

            failureDomain?.cancel()
            scheduler.terminate()
        }

        runBlocking {
            system.run()
            system.terminate()
        }
    }
}

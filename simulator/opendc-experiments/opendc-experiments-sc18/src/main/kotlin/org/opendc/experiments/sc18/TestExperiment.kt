/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.sc18

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestCoroutineScope
import org.opendc.compute.core.metal.service.ProvisioningService
import org.opendc.compute.simulator.SimVirtProvisioningService
import org.opendc.compute.simulator.allocation.NumberOfActiveServersAllocationPolicy
import org.opendc.format.environment.sc18.Sc18EnvironmentReader
import org.opendc.format.trace.gwf.GwfTraceReader
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.trace.core.EventTracer
import org.opendc.workflows.service.StageWorkflowService
import org.opendc.workflows.service.WorkflowEvent
import org.opendc.workflows.service.WorkflowSchedulerMode
import org.opendc.workflows.service.stage.job.NullJobAdmissionPolicy
import org.opendc.workflows.service.stage.job.SubmissionTimeJobOrderPolicy
import org.opendc.workflows.service.stage.task.NullTaskEligibilityPolicy
import org.opendc.workflows.service.stage.task.SubmissionTimeTaskOrderPolicy
import java.io.File
import kotlin.math.max

/**
 * Main entry point of the experiment.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("error: Please provide path to GWF trace")
        return
    }

    var total = 0
    var finished = 0

    val token = Channel<Boolean>()
    val testScope = TestCoroutineScope()
    val clock = DelayControllerClockAdapter(testScope)
    val tracer = EventTracer(clock)

    val schedulerAsync = testScope.async {
        val environment = Sc18EnvironmentReader(object {}.javaClass.getResourceAsStream("/environment.json"))
            .use { it.construct(testScope, clock) }

        val bareMetal = environment.platforms[0].zones[0].services[ProvisioningService]

        // Wait for the bare metal nodes to be spawned
        delay(10)

        val provisioner = SimVirtProvisioningService(testScope, clock, bareMetal, NumberOfActiveServersAllocationPolicy(), tracer, schedulingQuantum = 1000)

        // Wait for the hypervisors to be spawned
        delay(10)

        StageWorkflowService(
            testScope,
            clock,
            tracer,
            provisioner,
            mode = WorkflowSchedulerMode.Batch(100),
            jobAdmissionPolicy = NullJobAdmissionPolicy,
            jobOrderPolicy = SubmissionTimeJobOrderPolicy(),
            taskEligibilityPolicy = NullTaskEligibilityPolicy,
            taskOrderPolicy = SubmissionTimeTaskOrderPolicy(),
        )
    }

    testScope.launch {
        val scheduler = schedulerAsync.await()
        scheduler.events
            .onEach { event ->
                when (event) {
                    is WorkflowEvent.JobStarted -> {
                        println("Job ${event.job.uid} started")
                    }
                    is WorkflowEvent.JobFinished -> {
                        finished += 1
                        println("Jobs $finished/$total finished (${event.job.tasks.size} tasks)")

                        if (finished == total) {
                            token.send(true)
                        }
                    }
                }
            }
            .collect()
    }

    testScope.launch {
        val reader = GwfTraceReader(File(args[0]))
        val scheduler = schedulerAsync.await()

        while (reader.hasNext()) {
            val (time, job) = reader.next()
            total += 1
            delay(max(0, time * 1000 - clock.millis()))
            scheduler.submit(job)
        }
    }

    testScope.advanceUntilIdle()
}

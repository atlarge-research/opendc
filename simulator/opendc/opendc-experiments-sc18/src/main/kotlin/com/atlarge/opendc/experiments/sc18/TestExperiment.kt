/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.experiments.sc18

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.format.environment.sc18.Sc18EnvironmentReader
import com.atlarge.opendc.format.trace.gwf.GwfTraceReader
import com.atlarge.opendc.workflows.service.StageWorkflowService
import com.atlarge.opendc.workflows.service.WorkflowEvent
import com.atlarge.opendc.workflows.service.WorkflowSchedulerMode
import com.atlarge.opendc.workflows.service.stage.job.NullJobAdmissionPolicy
import com.atlarge.opendc.workflows.service.stage.job.SubmissionTimeJobOrderPolicy
import com.atlarge.opendc.workflows.service.stage.resource.FirstFitResourceSelectionPolicy
import com.atlarge.opendc.workflows.service.stage.resource.FunctionalResourceFilterPolicy
import com.atlarge.opendc.workflows.service.stage.task.NullTaskEligibilityPolicy
import com.atlarge.opendc.workflows.service.stage.task.SubmissionTimeTaskOrderPolicy
import kotlin.math.max
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.ServiceLoader

/**
 * Main entry point of the experiment.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("error: Please provide path to GWF trace")
        return
    }

    var total = 0
    var finished = 0

    val token = Channel<Boolean>()
    val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
    val system = provider(name = "sim")

    val schedulerDomain = system.newDomain(name = "scheduler")
    val schedulerAsync = schedulerDomain.async {
        val environment = Sc18EnvironmentReader(object {}.javaClass.getResourceAsStream("/env/setup-test.json"))
            .use { it.construct(system.newDomain("topology")) }

        StageWorkflowService(
            schedulerDomain,
            environment.platforms[0].zones[0].services[ProvisioningService],
            mode = WorkflowSchedulerMode.Batch(100),
            jobAdmissionPolicy = NullJobAdmissionPolicy,
            jobOrderPolicy = SubmissionTimeJobOrderPolicy(),
            taskEligibilityPolicy = NullTaskEligibilityPolicy,
            taskOrderPolicy = SubmissionTimeTaskOrderPolicy(),
            resourceFilterPolicy = FunctionalResourceFilterPolicy,
            resourceSelectionPolicy = FirstFitResourceSelectionPolicy
        )
    }

    val broker = system.newDomain(name = "broker")

    broker.launch {
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
    broker.launch {
        val ctx = simulationContext
        val reader = GwfTraceReader(File(args[0]))
        val scheduler = schedulerAsync.await()

        while (reader.hasNext()) {
            val (time, job) = reader.next()
            total += 1
            delay(max(0, time * 1000 - ctx.clock.millis()))
            scheduler.submit(job)
        }
    }

    runBlocking {
        system.run()
        system.terminate()
    }
}

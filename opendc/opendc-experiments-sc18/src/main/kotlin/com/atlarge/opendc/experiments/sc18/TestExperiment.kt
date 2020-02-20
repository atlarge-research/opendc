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
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.format.environment.sc18.Sc18EnvironmentReader
import com.atlarge.opendc.format.trace.gwf.GwfTraceReader
import com.atlarge.opendc.workflows.monitor.WorkflowMonitor
import com.atlarge.opendc.workflows.service.StageWorkflowService
import com.atlarge.opendc.workflows.service.WorkflowSchedulerMode
import com.atlarge.opendc.workflows.service.stage.job.SubmissionTimeJobOrderPolicy
import com.atlarge.opendc.workflows.service.stage.job.NullJobAdmissionPolicy
import com.atlarge.opendc.workflows.service.stage.resource.FirstFitResourceSelectionPolicy
import com.atlarge.opendc.workflows.service.stage.resource.FunctionalResourceDynamicFilterPolicy
import com.atlarge.opendc.workflows.service.stage.task.FifoTaskSortingPolicy
import com.atlarge.opendc.workflows.service.stage.task.FunctionalTaskEligibilityPolicy
import com.atlarge.opendc.workflows.workload.Job
import com.atlarge.opendc.workflows.workload.Task
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.ServiceLoader
import kotlin.math.max

/**
 * Main entry point of the experiment.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("error: Please provide path to GWF trace")
        return
    }

    val environment = Sc18EnvironmentReader(object {}.javaClass.getResourceAsStream("/env/setup-test.json"))
        .use { it.read() }

    var total = 0
    var finished = 0

    val token = Channel<Boolean>()

    val monitor = object : WorkflowMonitor {
        override suspend fun onJobStart(job: Job, time: Long) {
            println("Job ${job.uid} started")
        }

        override suspend fun onJobFinish(job: Job, time: Long) {
            finished += 1
            println("Jobs $finished/$total finished (${job.tasks.size} tasks)")

            if (finished == total) {
                token.send(true)
            }
        }

        override suspend fun onTaskStart(job: Job, task: Task, time: Long) {
        }

        override suspend fun onTaskFinish(job: Job, task: Task, status: Int, time: Long) {
        }
    }

    val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
    val system = provider({ ctx ->
        println(ctx.clock.instant())
        val scheduler = StageWorkflowService(
            ctx,
            environment.platforms[0].zones[0].services[ProvisioningService.Key],
            mode = WorkflowSchedulerMode.Batch(100),
            jobAdmissionPolicy = NullJobAdmissionPolicy,
            jobSortingPolicy = SubmissionTimeJobOrderPolicy(),
            taskEligibilityPolicy = FunctionalTaskEligibilityPolicy(),
            taskSortingPolicy = FifoTaskSortingPolicy(),
            resourceDynamicFilterPolicy = FunctionalResourceDynamicFilterPolicy(),
            resourceSelectionPolicy = FirstFitResourceSelectionPolicy()
        )

        val reader = GwfTraceReader(File(args[0]))

        while (reader.hasNext()) {
            val (time, job) = reader.next()
            total += 1
            delay(max(0, time * 1000 - ctx.clock.millis()))
            scheduler.submit(job, monitor)
        }

        token.receive()

        println(ctx.clock.instant())
    }, name = "sim")

    runBlocking {
        system.run()
        system.terminate()
    }
}

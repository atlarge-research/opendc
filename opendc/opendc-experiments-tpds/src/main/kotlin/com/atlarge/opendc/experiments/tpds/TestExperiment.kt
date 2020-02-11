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

package com.atlarge.opendc.experiments.tpds

import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.SendRef
import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.core.Broker
import com.atlarge.opendc.core.Model
import com.atlarge.opendc.core.PlatformMessage
import com.atlarge.opendc.core.find
import com.atlarge.opendc.core.services.provisioning.SimpleProvisioningService
import com.atlarge.opendc.core.services.resources.ResourceManagementService
import com.atlarge.opendc.core.zones
import com.atlarge.opendc.format.environment.sc18.Sc18EnvironmentReader
import com.atlarge.opendc.format.trace.gwf.GwfTraceReader
import com.atlarge.opendc.workflows.service.StageWorkflowScheduler
import com.atlarge.opendc.workflows.service.WorkflowEvent
import com.atlarge.opendc.workflows.service.WorkflowMessage
import com.atlarge.opendc.workflows.service.WorkflowSchedulerMode
import com.atlarge.opendc.workflows.service.WorkflowService
import com.atlarge.opendc.workflows.service.stage.job.FifoJobSortingPolicy
import com.atlarge.opendc.workflows.service.stage.job.NullJobAdmissionPolicy
import com.atlarge.opendc.workflows.service.stage.resource.FirstFitResourceSelectionPolicy
import com.atlarge.opendc.workflows.service.stage.resource.FunctionalResourceDynamicFilterPolicy
import com.atlarge.opendc.workflows.service.stage.task.FifoTaskSortingPolicy
import com.atlarge.opendc.workflows.service.stage.task.FunctionalTaskEligibilityPolicy
import com.atlarge.opendc.workflows.workload.Job
import java.io.File
import java.util.ServiceLoader
import kotlin.math.max
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Main entry point of the experiment.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("error: Please provide path to GWF trace")
        return
    }

    val scheduler = StageWorkflowScheduler(
        mode = WorkflowSchedulerMode.Batch(100),
        jobAdmissionPolicy = NullJobAdmissionPolicy,
        jobSortingPolicy = FifoJobSortingPolicy(),
        taskEligibilityPolicy = FunctionalTaskEligibilityPolicy(),
        taskSortingPolicy = FifoTaskSortingPolicy(),
        resourceDynamicFilterPolicy = FunctionalResourceDynamicFilterPolicy(),
        resourceSelectionPolicy = FirstFitResourceSelectionPolicy()
    )

    val environment = Sc18EnvironmentReader(object {}.javaClass.getResourceAsStream("/env/setup-test.json"))
        .use { it.read() }
        .let { env ->
            env.copy(platforms = env.platforms.map { platform ->
                platform.copy(zones = platform.zones.map { zone ->
                    val services = zone.services + setOf(ResourceManagementService, SimpleProvisioningService, WorkflowService(scheduler))
                    zone.copy(services = services)
                })
            })
        }

    val broker = object : Broker {
        override suspend fun invoke(ctx: ProcessContext, platforms: List<SendRef<PlatformMessage>>) {
            coroutineScope {
                val zones = platforms.first().zones()
                val service = zones.values.first().find(WorkflowService)
                val activeJobs = mutableSetOf<Job>()
                val channel = ctx.open<WorkflowEvent>()
                val outlet = ctx.connect(service)
                val inlet = ctx.listen(channel.receive)

                launch {
                    val reader = GwfTraceReader(File(args[0]))

                    while (reader.hasNext() && isActive) {
                        val (time, job) = reader.next()
                        delay(max(0, time - ctx.clock.millis()))
                        outlet.send(WorkflowMessage.Submit(job, channel.send))
                    }
                }

                var total = 0
                var finished = 0

                while (isActive) {
                    when (val msg = inlet.receive()) {
                        is WorkflowEvent.JobSubmitted -> {
                            println("Job ${msg.job.uid} submitted")
                            total += 1
                        }
                        is WorkflowEvent.JobStarted -> {
                            activeJobs += msg.job
                        }
                        is WorkflowEvent.JobFinished -> {
                            activeJobs -= msg.job
                            finished += 1
                            println("Jobs $finished/$total finished (${msg.job.tasks.size} tasks)")
                            if (activeJobs.isEmpty())
                                return@coroutineScope
                        }
                    }
                }
            }
        }
    }

    val model = Model(environment, listOf(broker))
    val factory = ServiceLoader.load(SimulationEngineProvider::class.java).first()
    val system = factory({ model(it) }, name = "sim")

    runBlocking {
        system.run()
        system.terminate()
    }
}

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

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorSystemFactory
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.TimerScheduler
import com.atlarge.odcsim.coroutines.suspending
import com.atlarge.odcsim.receiveMessage
import com.atlarge.odcsim.same
import com.atlarge.odcsim.stopped
import com.atlarge.odcsim.unhandled
import com.atlarge.odcsim.withTimers
import com.atlarge.opendc.format.environment.sc18.Sc18EnvironmentReader
import com.atlarge.opendc.format.trace.gwf.GwfTraceReader
import com.atlarge.opendc.model.Broker
import com.atlarge.opendc.model.Model
import com.atlarge.opendc.model.PlatformRef
import com.atlarge.opendc.model.find
import com.atlarge.opendc.model.services.provisioning.SimpleProvisioningService
import com.atlarge.opendc.model.services.resources.ResourceManagementService
import com.atlarge.opendc.model.services.workflows.StageWorkflowScheduler
import com.atlarge.opendc.model.services.workflows.WorkflowEvent
import com.atlarge.opendc.model.services.workflows.WorkflowMessage
import com.atlarge.opendc.model.services.workflows.WorkflowSchedulerMode
import com.atlarge.opendc.model.services.workflows.WorkflowService
import com.atlarge.opendc.model.services.workflows.stages.job.FifoJobSortingPolicy
import com.atlarge.opendc.model.services.workflows.stages.job.NullJobAdmissionPolicy
import com.atlarge.opendc.model.services.workflows.stages.resources.FirstFitResourceSelectionPolicy
import com.atlarge.opendc.model.services.workflows.stages.resources.FunctionalResourceDynamicFilterPolicy
import com.atlarge.opendc.model.services.workflows.stages.task.FifoTaskSortingPolicy
import com.atlarge.opendc.model.services.workflows.stages.task.FunctionalTaskEligibilityPolicy
import com.atlarge.opendc.model.workload.workflow.Job
import com.atlarge.opendc.model.zones
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


    val scheduler = StageWorkflowScheduler(
        mode = WorkflowSchedulerMode.Batch(100.0),
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
        override fun invoke(platforms: List<PlatformRef>): Behavior<*> = suspending<Any> { ctx ->
            val zones = platforms.first().zones()
            val service = zones.values.first().find(WorkflowService)

            val activeJobs = mutableSetOf<Job>()
            val reader = GwfTraceReader(File(args[0]))

            fun submitNext(ctx: ActorContext<Any>, timers: TimerScheduler<Any>) {
                if (!reader.hasNext()) {
                    return
                }

                val (time, job) = reader.next()
                timers.after(job, max(.0, time - ctx.time)) {
                    ctx.send(service, WorkflowMessage.Submit(job, ctx.self))
                    submitNext(ctx, timers)
                }
            }

            var total = 0
            var finished = 0

            withTimers { timers ->
                submitNext(ctx, timers)
                receiveMessage { msg ->
                    when (msg) {
                        is WorkflowEvent.JobSubmitted -> {
                            ctx.log.info("Job {} submitted", msg.job.uid)
                            total += 1
                            same()
                        }
                        is WorkflowEvent.JobStarted -> {
                            activeJobs += msg.job
                            same()
                        }
                        is WorkflowEvent.JobFinished -> {
                            activeJobs -= msg.job
                            finished += 1
                            ctx.log.info("Jobs {}/{} finished ({} tasks)", finished, total, msg.job.tasks.size)
                            if (activeJobs.isEmpty()) stopped() else same()
                        }
                        else ->
                            unhandled()
                    }
                }
            }
        }
    }

    val model = Model(environment, listOf(broker))
    val factory = ServiceLoader.load(ActorSystemFactory::class.java).first()
    val system = factory(model(), name = "sim")
    system.run()
    system.terminate()
}

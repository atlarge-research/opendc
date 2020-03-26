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

package com.atlarge.opendc.workflows.service

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.format.environment.sc18.Sc18EnvironmentReader
import com.atlarge.opendc.format.trace.gwf.GwfTraceReader
import com.atlarge.opendc.workflows.service.stage.job.NullJobAdmissionPolicy
import com.atlarge.opendc.workflows.service.stage.job.SubmissionTimeJobOrderPolicy
import com.atlarge.opendc.workflows.service.stage.resource.FirstFitResourceSelectionPolicy
import com.atlarge.opendc.workflows.service.stage.resource.FunctionalResourceFilterPolicy
import com.atlarge.opendc.workflows.service.stage.task.NullTaskEligibilityPolicy
import com.atlarge.opendc.workflows.service.stage.task.SubmissionTimeTaskOrderPolicy
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.ServiceLoader
import kotlin.math.max

/**
 * Integration test suite for the [StageWorkflowService].
 */
@DisplayName("StageWorkflowService")
internal class StageWorkflowSchedulerIntegrationTest {
    /**
     * A large integration test where we check whether all tasks in some trace are executed correctly.
     */
    @Test
    fun `should execute all tasks in trace`() {
        var jobsSubmitted = 0L
        var jobsStarted = 0L
        var jobsFinished = 0L
        var tasksStarted = 0L
        var tasksFinished = 0L

        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider(name = "sim")

        val schedulerDomain = system.newDomain(name = "scheduler")
        val schedulerAsync = schedulerDomain.async {
            val environment = Sc18EnvironmentReader(object {}.javaClass.getResourceAsStream("/environment.json"))
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
                        is WorkflowEvent.JobStarted -> jobsStarted++
                        is WorkflowEvent.JobFinished -> jobsFinished++
                        is WorkflowEvent.TaskStarted -> tasksStarted++
                        is WorkflowEvent.TaskFinished -> tasksFinished++
                    }
                }
                .collect()
        }

        broker.launch {
            val ctx = simulationContext
            val reader = GwfTraceReader(object {}.javaClass.getResourceAsStream("/trace.gwf"))
            val scheduler = schedulerAsync.await()

            while (reader.hasNext()) {
                val (time, job) = reader.next()
                jobsSubmitted++
                delay(max(0, time * 1000 - ctx.clock.millis()))
                scheduler.submit(job)
            }
        }

        runBlocking {
            system.run()
            system.terminate()
        }

        assertEquals(jobsSubmitted, jobsStarted, "Not all submitted jobs started")
        assertEquals(jobsSubmitted, jobsFinished, "Not all started jobs finished")
        assertEquals(tasksStarted, tasksFinished, "Not all started tasks finished")
    }
}

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

package org.opendc.workflows.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.NumberOfActiveServersAllocationPolicy
import org.opendc.compute.simulator.SimHost
import org.opendc.format.environment.sc18.Sc18EnvironmentReader
import org.opendc.format.trace.gwf.GwfTraceReader
import org.opendc.simulator.compute.SimSpaceSharedHypervisorProvider
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.trace.core.EventTracer
import org.opendc.workflows.service.stage.job.NullJobAdmissionPolicy
import org.opendc.workflows.service.stage.job.SubmissionTimeJobOrderPolicy
import org.opendc.workflows.service.stage.task.NullTaskEligibilityPolicy
import org.opendc.workflows.service.stage.task.SubmissionTimeTaskOrderPolicy
import kotlin.math.max

/**
 * Integration test suite for the [StageWorkflowService].
 */
@DisplayName("StageWorkflowService")
@OptIn(ExperimentalCoroutinesApi::class)
internal class StageWorkflowSchedulerIntegrationTest {
    /**
     * A large integration test where we check whether all tasks in some trace are executed correctly.
     */
    @Test
    fun testTrace() {
        var jobsSubmitted = 0L
        var jobsStarted = 0L
        var jobsFinished = 0L
        var tasksStarted = 0L
        var tasksFinished = 0L

        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)
        val tracer = EventTracer(clock)

        val scheduler = let {
            val hosts = Sc18EnvironmentReader(object {}.javaClass.getResourceAsStream("/environment.json"))
                .use { it.read() }
                .map { def ->
                    SimHost(
                        def.uid,
                        def.name,
                        def.model,
                        def.meta,
                        testScope.coroutineContext,
                        clock,
                        SimSpaceSharedHypervisorProvider()
                    )
                }

            val compute = ComputeService(testScope.coroutineContext, clock, tracer, NumberOfActiveServersAllocationPolicy(), schedulingQuantum = 1000)

            hosts.forEach { compute.addHost(it) }

            StageWorkflowService(
                testScope,
                clock,
                tracer,
                compute.newClient(),
                mode = WorkflowSchedulerMode.Batch(100),
                jobAdmissionPolicy = NullJobAdmissionPolicy,
                jobOrderPolicy = SubmissionTimeJobOrderPolicy(),
                taskEligibilityPolicy = NullTaskEligibilityPolicy,
                taskOrderPolicy = SubmissionTimeTaskOrderPolicy(),
            )
        }

        testScope.launch {
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

        testScope.launch {
            val reader = GwfTraceReader(object {}.javaClass.getResourceAsStream("/trace.gwf"))

            while (reader.hasNext()) {
                val entry = reader.next()
                jobsSubmitted++
                delay(max(0, entry.start - clock.millis()))
                scheduler.submit(entry.workload)
            }
        }

        testScope.advanceUntilIdle()

        assertAll(
            { assertEquals(emptyList<Throwable>(), testScope.uncaughtExceptions) },
            { assertNotEquals(0, jobsSubmitted, "No jobs submitted") },
            { assertEquals(jobsSubmitted, jobsStarted, "Not all submitted jobs started") },
            { assertEquals(jobsSubmitted, jobsFinished, "Not all started jobs finished") },
            { assertEquals(tasksStarted, tasksFinished, "Not all started tasks finished") }
        )
    }
}

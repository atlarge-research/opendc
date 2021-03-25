/*
 * Copyright (c) 2021 AtLarge Research
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
import kotlinx.coroutines.test.TestCoroutineScope
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.NumberOfActiveServersAllocationPolicy
import org.opendc.compute.simulator.SimHost
import org.opendc.format.environment.sc18.Sc18EnvironmentReader
import org.opendc.format.trace.gwf.GwfTraceReader
import org.opendc.harness.dsl.Experiment
import org.opendc.harness.dsl.anyOf
import org.opendc.simulator.compute.SimSpaceSharedHypervisorProvider
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.trace.core.EventTracer
import org.opendc.trace.core.enable
import org.opendc.workflow.service.WorkflowEvent
import org.opendc.workflow.service.WorkflowService
import org.opendc.workflow.service.scheduler.WorkflowSchedulerMode
import org.opendc.workflow.service.scheduler.job.NullJobAdmissionPolicy
import org.opendc.workflow.service.scheduler.job.SubmissionTimeJobOrderPolicy
import org.opendc.workflow.service.scheduler.task.NullTaskEligibilityPolicy
import org.opendc.workflow.service.scheduler.task.SubmissionTimeTaskOrderPolicy
import java.io.File
import java.io.FileInputStream
import kotlin.math.max

/**
 * The [UnderspecificationExperiment] investigates the impact of scheduler underspecification on  performance.
 * It focuses on components that must exist (that is, based on their own publications, the correct operation of the
 * schedulers under study requires these components), yet have been left underspecified by their author.
 */
public class UnderspecificationExperiment : Experiment("underspecification") {
    /**
     * The workflow traces to test.
     */
    private val trace: String by anyOf("input/traces/chronos_exp_noscaler_ca.gwf")

    /**
     * The datacenter environments to test.
     */
    private val environment: String by anyOf("input/environments/base.json")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun doRun(repeat: Int) {
        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)
        val tracer = EventTracer(clock)
        val recording = tracer.openRecording().run {
            enable<WorkflowEvent.JobSubmitted>()
            enable<WorkflowEvent.JobStarted>()
            enable<WorkflowEvent.JobFinished>()
            enable<WorkflowEvent.TaskStarted>()
            enable<WorkflowEvent.TaskFinished>()
            this
        }

        testScope.launch {
            launch { println("MAKESPAN: ${recording.workflowRuntime()}") }
            launch { println("WAIT: ${recording.workflowWaitingTime()}") }
            recording.start()
        }

        testScope.launch {
            val hosts = Sc18EnvironmentReader(FileInputStream(File(environment)))
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

            val compute = ComputeService(
                testScope.coroutineContext,
                clock,
                NumberOfActiveServersAllocationPolicy(),
            )

            hosts.forEach { compute.addHost(it) }

            val scheduler = WorkflowService(
                testScope.coroutineContext,
                clock,
                tracer,
                compute.newClient(),
                mode = WorkflowSchedulerMode.Batch(100),
                jobAdmissionPolicy = NullJobAdmissionPolicy,
                jobOrderPolicy = SubmissionTimeJobOrderPolicy(),
                taskEligibilityPolicy = NullTaskEligibilityPolicy,
                taskOrderPolicy = SubmissionTimeTaskOrderPolicy(),
            )

            val reader = GwfTraceReader(File(trace))

            while (reader.hasNext()) {
                val entry = reader.next()
                delay(max(0, entry.start * 1000 - clock.millis()))
                scheduler.submit(entry.workload)
            }
        }

        testScope.advanceUntilIdle()
        recording.close()

        // Check whether everything went okay
        testScope.uncaughtExceptions.forEach { it.printStackTrace() }
        assert(testScope.uncaughtExceptions.isEmpty()) { "Errors occurred during execution of the experiment" }
    }
}

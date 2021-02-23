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
import org.opendc.compute.core.metal.service.ProvisioningService
import org.opendc.compute.simulator.SimVirtProvisioningService
import org.opendc.compute.simulator.allocation.NumberOfActiveServersAllocationPolicy
import org.opendc.format.environment.sc18.Sc18EnvironmentReader
import org.opendc.format.trace.gwf.GwfTraceReader
import org.opendc.harness.dsl.Experiment
import org.opendc.harness.dsl.anyOf
import org.opendc.simulator.compute.SimSpaceSharedHypervisorProvider
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.trace.core.EventTracer
import org.opendc.trace.core.enable
import org.opendc.workflows.service.StageWorkflowService
import org.opendc.workflows.service.WorkflowEvent
import org.opendc.workflows.service.WorkflowSchedulerMode
import org.opendc.workflows.service.stage.job.NullJobAdmissionPolicy
import org.opendc.workflows.service.stage.job.SubmissionTimeJobOrderPolicy
import org.opendc.workflows.service.stage.task.NullTaskEligibilityPolicy
import org.opendc.workflows.service.stage.task.SubmissionTimeTaskOrderPolicy
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
    private val trace: String by anyOf("traces/chronos_exp_noscaler_ca.gwf")

    /**
     * The datacenter environments to test.
     */
    private val environment: String by anyOf("environments/base.json")

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
            val environment = Sc18EnvironmentReader(FileInputStream(File(environment)))
                .use { it.construct(testScope, clock) }

            val bareMetal = environment.platforms[0].zones[0].services[ProvisioningService]

            // Wait for the bare metal nodes to be spawned
            delay(10)

            val provisioner = SimVirtProvisioningService(
                testScope,
                clock,
                bareMetal,
                NumberOfActiveServersAllocationPolicy(),
                tracer,
                SimSpaceSharedHypervisorProvider(),
                schedulingQuantum = 1000
            )

            // Wait for the hypervisors to be spawned
            delay(10)

            val scheduler = StageWorkflowService(
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

            val reader = GwfTraceReader(File(trace))

            while (reader.hasNext()) {
                val (time, job) = reader.next()
                delay(max(0, time * 1000 - clock.millis()))
                scheduler.submit(job)
            }
        }

        testScope.advanceUntilIdle()
        recording.close()

        // Check whether everything went okay
        testScope.uncaughtExceptions.forEach { it.printStackTrace() }
        assert(testScope.uncaughtExceptions.isEmpty()) { "Errors occurred during execution of the experiment" }
    }
}

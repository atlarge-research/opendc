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

package org.opendc.workflow.service

import io.opentelemetry.sdk.metrics.export.MetricProducer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.VCpuWeigher
import org.opendc.compute.workload.ComputeServiceHelper
import org.opendc.compute.workload.topology.HostSpec
import org.opendc.simulator.compute.kernel.SimSpaceSharedHypervisorProvider
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.ConstantPowerModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.trace.Trace
import org.opendc.workflow.service.scheduler.job.NullJobAdmissionPolicy
import org.opendc.workflow.service.scheduler.job.SubmissionTimeJobOrderPolicy
import org.opendc.workflow.service.scheduler.task.NullTaskEligibilityPolicy
import org.opendc.workflow.service.scheduler.task.SubmissionTimeTaskOrderPolicy
import org.opendc.workflow.workload.WorkflowSchedulerSpec
import org.opendc.workflow.workload.WorkflowServiceHelper
import org.opendc.workflow.workload.toJobs
import java.nio.file.Paths
import java.time.Duration
import java.util.*

/**
 * Integration test suite for the [WorkflowService].
 */
@DisplayName("WorkflowService")
internal class WorkflowServiceTest {
    /**
     * A large integration test where we check whether all tasks in some trace are executed correctly.
     */
    @Test
    fun testTrace() = runBlockingSimulation {
        // Configure the ComputeService that is responsible for mapping virtual machines onto physical hosts
        val HOST_COUNT = 4
        val computeScheduler = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(1.0), RamFilter(1.0)),
            weighers = listOf(VCpuWeigher(1.0, multiplier = 1.0))
        )
        val computeHelper = ComputeServiceHelper(coroutineContext, clock, computeScheduler, schedulingQuantum = Duration.ofSeconds(1))

        repeat(HOST_COUNT) { computeHelper.registerHost(createHostSpec(it)) }

        // Configure the WorkflowService that is responsible for scheduling the workflow tasks onto machines
        val workflowScheduler = WorkflowSchedulerSpec(
            schedulingQuantum = Duration.ofMillis(100),
            jobAdmissionPolicy = NullJobAdmissionPolicy,
            jobOrderPolicy = SubmissionTimeJobOrderPolicy(),
            taskEligibilityPolicy = NullTaskEligibilityPolicy,
            taskOrderPolicy = SubmissionTimeTaskOrderPolicy(),
        )
        val workflowHelper = WorkflowServiceHelper(coroutineContext, clock, computeHelper.service.newClient(), workflowScheduler)

        try {
            val trace = Trace.open(
                Paths.get(checkNotNull(WorkflowServiceTest::class.java.getResource("/trace.gwf")).toURI()),
                format = "gwf"
            )

            workflowHelper.replay(trace.toJobs())
        } finally {
            workflowHelper.close()
            computeHelper.close()
        }

        val metrics = collectMetrics(workflowHelper.metricProducer)

        assertAll(
            { assertEquals(758, metrics.jobsSubmitted, "No jobs submitted") },
            { assertEquals(0, metrics.jobsActive, "Not all submitted jobs started") },
            { assertEquals(metrics.jobsSubmitted, metrics.jobsFinished, "Not all started jobs finished") },
            { assertEquals(0, metrics.tasksActive, "Not all started tasks finished") },
            { assertEquals(metrics.tasksSubmitted, metrics.tasksFinished, "Not all started tasks finished") },
            { assertEquals(32649883L, clock.millis()) { "Total duration incorrect" } }
        )
    }

    /**
     * Construct a [HostSpec] for a simulated host.
     */
    private fun createHostSpec(uid: Int): HostSpec {
        // Machine model based on: https://www.spec.org/power_ssj2008/results/res2020q1/power_ssj2008-20191125-01012.html
        val node = ProcessingNode("AMD", "am64", "EPYC 7742", 32)
        val cpus = List(node.coreCount) { ProcessingUnit(node, it, 3400.0) }
        val memory = List(8) { MemoryUnit("Samsung", "Unknown", 2933.0, 16_000) }

        val machineModel = MachineModel(cpus, memory)

        return HostSpec(
            UUID(0, uid.toLong()),
            "host-$uid",
            emptyMap(),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0)),
            SimSpaceSharedHypervisorProvider()
        )
    }

    class WorkflowMetrics {
        var jobsSubmitted = 0L
        var jobsActive = 0L
        var jobsFinished = 0L
        var tasksSubmitted = 0L
        var tasksActive = 0L
        var tasksFinished = 0L
    }

    /**
     * Collect the metrics of the workflow service.
     */
    private fun collectMetrics(metricProducer: MetricProducer): WorkflowMetrics {
        val metrics = metricProducer.collectAllMetrics().associateBy { it.name }
        val res = WorkflowMetrics()
        res.jobsSubmitted = metrics["jobs.submitted"]?.longSumData?.points?.last()?.value ?: 0
        res.jobsActive = metrics["jobs.active"]?.longSumData?.points?.last()?.value ?: 0
        res.jobsFinished = metrics["jobs.finished"]?.longSumData?.points?.last()?.value ?: 0
        res.tasksSubmitted = metrics["tasks.submitted"]?.longSumData?.points?.last()?.value ?: 0
        res.tasksActive = metrics["tasks.active"]?.longSumData?.points?.last()?.value ?: 0
        res.tasksFinished = metrics["tasks.finished"]?.longSumData?.points?.last()?.value ?: 0
        return res
    }
}

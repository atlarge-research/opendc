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

import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.opendc.telemetry.sdk.toOtelClock
import org.opendc.workflow.service.internal.WorkflowServiceImpl
import org.opendc.workflow.service.scheduler.WorkflowSchedulerMode
import org.opendc.workflow.service.scheduler.job.NullJobAdmissionPolicy
import org.opendc.workflow.service.scheduler.job.SubmissionTimeJobOrderPolicy
import org.opendc.workflow.service.scheduler.task.NullTaskEligibilityPolicy
import org.opendc.workflow.service.scheduler.task.SubmissionTimeTaskOrderPolicy
import kotlin.math.max

/**
 * Integration test suite for the [WorkflowServiceImpl].
 */
@DisplayName("WorkflowServiceImpl")
@OptIn(ExperimentalCoroutinesApi::class)
internal class WorkflowServiceIntegrationTest {
    /**
     * A large integration test where we check whether all tasks in some trace are executed correctly.
     */
    @Test
    fun testTrace() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)

        val meterProvider: MeterProvider = SdkMeterProvider
            .builder()
            .setClock(clock.toOtelClock())
            .build()

        val hosts = Sc18EnvironmentReader(object {}.javaClass.getResourceAsStream("/environment.json"))
            .use { it.read() }
            .map { def ->
                SimHost(
                    def.uid,
                    def.name,
                    def.model,
                    def.meta,
                    coroutineContext,
                    clock,
                    MeterProvider.noop().get("opendc-compute-simulator"),
                    SimSpaceSharedHypervisorProvider()
                )
            }

        val meter = MeterProvider.noop().get("opendc-compute")
        val compute = ComputeService(coroutineContext, clock, meter, NumberOfActiveServersAllocationPolicy(), schedulingQuantum = 1000)

        hosts.forEach { compute.addHost(it) }

        val scheduler = WorkflowService(
            coroutineContext,
            clock,
            meterProvider.get("opendc-workflow"),
            compute.newClient(),
            mode = WorkflowSchedulerMode.Batch(100),
            jobAdmissionPolicy = NullJobAdmissionPolicy,
            jobOrderPolicy = SubmissionTimeJobOrderPolicy(),
            taskEligibilityPolicy = NullTaskEligibilityPolicy,
            taskOrderPolicy = SubmissionTimeTaskOrderPolicy(),
        )

        val reader = GwfTraceReader(object {}.javaClass.getResourceAsStream("/trace.gwf"))
        var offset = Long.MIN_VALUE

        coroutineScope {
            while (reader.hasNext()) {
                val entry = reader.next()

                if (offset < 0) {
                    offset = entry.start - clock.millis()
                }

                delay(max(0, (entry.start - offset) - clock.millis()))
                launch {
                    scheduler.run(entry.workload)
                }
            }
        }

        hosts.forEach(SimHost::close)
        scheduler.close()
        compute.close()

        val metrics = collectMetrics(meterProvider as MetricProducer)

        assertAll(
            { assertEquals(758, metrics.jobsSubmitted, "No jobs submitted") },
            { assertEquals(0, metrics.jobsActive, "Not all submitted jobs started") },
            { assertEquals(metrics.jobsSubmitted, metrics.jobsFinished, "Not all started jobs finished") },
            { assertEquals(0, metrics.tasksActive, "Not all started tasks finished") },
            { assertEquals(metrics.tasksSubmitted, metrics.tasksFinished, "Not all started tasks finished") }
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

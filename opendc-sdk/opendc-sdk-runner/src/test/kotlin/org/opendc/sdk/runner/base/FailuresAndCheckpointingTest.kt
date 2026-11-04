/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.checkpoint.CheckpointModelSpec
import org.opendc.sdk.model.failure.ConstantDistributionSpec
import org.opendc.sdk.model.failure.CustomFailureSpec
import org.opendc.sdk.model.failure.FailureSpec
import org.opendc.sdk.model.failure.InlineTraceBasedFailureSpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.GpuSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.ScalingPolicySpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.base.harness.FULL_LOAD_POWER
import org.opendc.sdk.runner.base.harness.HALF_LOAD_POWER
import org.opendc.sdk.runner.base.harness.HOST
import org.opendc.sdk.runner.base.harness.IDLE_POWER
import org.opendc.sdk.runner.base.harness.MINUTE
import org.opendc.sdk.runner.base.harness.SECOND
import org.opendc.sdk.runner.base.harness.assertDelays
import org.opendc.sdk.runner.base.harness.checkpointEveryMinute
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.energy
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest
import org.opendc.sdk.runner.base.harness.singleClusterTopology
import org.opendc.sdk.runner.base.harness.singleFailure

/**
 * Integration tests for host failures and task checkpointing.
 *
 * Every test runs on a single host with one 2 GHz core ([singleClusterTopology]). A task cannot move to another host
 * when its host fails, so it waits until the host recovers, which is reported as scheduling delay.
 */
class FailuresAndCheckpointingTest {
    private val topology = singleClusterTopology()

    /** The host fails after every 5 min of uptime, for 5 min. */
    private val repeatedFailures = singleFailure.copy(repeat = true)

    /**
     * A single 10 min task runs with a failure trace whose first failure occurs after 500 min.
     * The failure happens long after the task has finished, so the simulation ends after 10 min without any delays.
     */
    @Test
    fun `a failure after the task has finished has no effect`() {
        val failureModel = InlineTraceBasedFailureSpec(listOf(FailureSpec(TimeDelta.ofMin(500), TimeDelta.ofMin(5), 1.0)), repeat = false)

        val monitor = runTest(topology, listOf(tenMinuteTask()), failureModel)

        val runtime = 10 * MINUTE
        val activeTime = runtime / 2
        val oneMinuteEnergyUsage = energy(HALF_LOAD_POWER, MINUTE)
        val energyUsage = energy(HALF_LOAD_POWER, runtime)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(runtime - activeTime, monitor.hostCpuIdleTimes.getValue(HOST).last()) { "Idle time incorrect" } },
            { assertEquals(activeTime, monitor.hostCpuActiveTimes.getValue(HOST).last()) { "Active time incorrect" } },
            { assertEquals(oneMinuteEnergyUsage, monitor.hostEnergyUsages.getValue(HOST)[0]) { "Energy usage at sample 0 incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            // The failure does not impact the task, so there are no delays
            { assertDelays(monitor, taskId = 0, scheduling = 0, failure = 0, checkpoint = 0) },
        )
    }

    /**
     * A single 10 min task runs with a failure trace whose first failure occurs after exactly 10 min, the moment the task
     * finishes. The task has done all its work by then, so it should complete instead of being restarted.
     *
     * TODO: OpenDC does not return the correct values. This will be solved later, and has been marked as a bug
     */
    @Test @Disabled
    fun `a failure at the moment the task finishes has no effect`() {
        val failureModel = InlineTraceBasedFailureSpec(listOf(FailureSpec(TimeDelta.ofMin(10), TimeDelta.ofMin(5), 1.0)), repeat = false)

        val monitor = runTest(topology, listOf(tenMinuteTask()), failureModel)

        assertAll(
            { assertEquals(10 * MINUTE, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = 0, failure = 0, checkpoint = 0) },
        )
    }

    /**
     * A single 10 min task runs with a repeating failure trace that contains no failures.
     * Replaying the empty trace should not inject any failures, nor stop the simulation from progressing.
     */
    @Test
    fun `an empty repeating failure trace injects no failures`() {
        val failureModel = InlineTraceBasedFailureSpec(failures = emptyList(), repeat = true)

        val monitor = runTest(topology, listOf(tenMinuteTask()), failureModel)

        val runtime = 10 * MINUTE

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(runtime / 2, monitor.hostCpuActiveTimes.getValue(HOST).last()) { "Active time incorrect" } },
        )
    }

    /**
     * A single 10 min task is interrupted by a failure after 5 min.
     * Without checkpointing, the 5 min of work is lost and the full task has to be rerun.
     * The host is failed for 5 min, so the task restarts at min 10 and finishes at min 20.
     */
    @Test
    fun `a single failure restarts the task from scratch`() {
        val monitor = runTest(topology, listOf(tenMinuteTask()), singleFailure)

        // We expect a scheduling delay of 5 min because after the failure, no host is available for 5 min.
        // We expect a failure delay of 5 min because the failure happens after 5 min, and all that work is lost.
        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 5 * MINUTE

        // The task runs for its 10 min plus the work it redoes. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay
        val runtime = runningTime + expectedSchedulingDelay
        val activeTime = runningTime / 2
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(runtime - activeTime, monitor.hostCpuIdleTimes.getValue(HOST).last()) { "Idle time incorrect" } },
            { assertEquals(activeTime, monitor.hostCpuActiveTimes.getValue(HOST).last()) { "Active time incorrect" } },
            { assertEquals(HALF_LOAD_POWER, monitor.hostPowerDraws.getValue(HOST)[0]) { "Power draw at sample 0 incorrect" } },
            { assertEquals(IDLE_POWER, monitor.hostPowerDraws.getValue(HOST)[5]) { "Power draw at sample 5 incorrect" } },
            { assertEquals(HALF_LOAD_POWER, monitor.hostPowerDraws.getValue(HOST)[10]) { "Power draw at sample 10 incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
        )
    }

    /**
     * A 10 min task consists of a 3 min and a 7 min fragment, and is interrupted by a failure after 5 min, 2 min into its
     * second fragment.
     * Without checkpointing, the work of both fragments is lost: all 3 min of the first and 2 min of the second.
     */
    @Test
    fun `a failure in a later fragment loses the work of all earlier fragments`() {
        val workload = listOf(createTestTask(id = 0, fragments = listOf(fragment(3 * MINUTE, 1000.0), fragment(7 * MINUTE, 1000.0))))

        val monitor = runTest(topology, workload, singleFailure)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 3 * MINUTE + 2 * MINUTE

        // The task runs for its 10 min plus the work it redoes. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
        )
    }

    /**
     * Like `a single failure restarts the task from scratch`, but the task also uses the GPUs of the host, at 1000 MHz
     * per GPU. The work lost at the failure should be counted once, not once for every resource the task uses.
     *
     * When the host recovers, it is rebuilt with all its GPUs, so the restarted task again gets 1000 MHz from every GPU.
     * This is tested with a single GPU, and with more than four GPUs.
     */
    @ParameterizedTest(name = "{0} GPUs")
    @ValueSource(ints = [1, 5])
    fun `a failure restarts a task that uses both CPU and GPU from scratch`(gpuCount: Int) {
        val gpuTopology =
            TopologySpec(
                datacenters =
                    listOf(
                        DataCenterSpec(
                            clusters =
                                listOf(
                                    ClusterSpec(
                                        name = "C01",
                                        hosts =
                                            listOf(
                                                HostSpec(
                                                    name = HOST,
                                                    cpu = CpuSpec(coreCount = 1, coreSpeed = Frequency.ofGHz(2)),
                                                    memory = MemorySpec(size = DataSize.ofGB(128)),
                                                    gpu = GpuSpec(coreCount = 1, coreSpeed = Frequency.ofGHz(2), count = gpuCount),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        val workload =
            listOf(createTestTask(id = 0, fragments = listOf(fragment(10 * MINUTE, 1000.0, gpuCount * 1000.0)), gpuCoreCount = 1))

        val monitor = runTest(gpuTopology, workload, singleFailure)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 5 * MINUTE

        val runtime = 10 * MINUTE + expectedFailureDelay + expectedSchedulingDelay
        val gpuSupplies = List(gpuCount) { 1000.0 }

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            // The task runs on all GPUs before the failure (sample 0), and again after it restarts at min 10 (sample 10)
            { assertEquals(gpuSupplies, monitor.hostGpuSupplied.getValue(HOST)[0]) { "GPU supplies at sample 0 incorrect" } },
            { assertEquals(gpuSupplies, monitor.hostGpuSupplied.getValue(HOST)[10]) { "GPU supplies at sample 10 incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
        )
    }

    /**
     * Like `a single failure restarts the task from scratch`, but with the perfect scaling policy, which measures the
     * work of a task as demand times duration (MHz times ms) instead of as a duration.
     * The failure delay should still be the 5 min of lost work.
     */
    @Test
    fun `a failure restarts a task with perfect scaling from scratch`() {
        val monitor = runTest(topology, listOf(tenMinuteTask()), singleFailure, scalingPolicy = ScalingPolicySpec.Perfect)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 5 * MINUTE

        // The task runs for its 10 min plus the work it redoes. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
        )
    }

    /**
     * A single 10 min task is interrupted by two failures.
     * The first failure happens at min 5 and lasts 5 min.
     * The second failure happens at min 17, after the task has run another 7 min, and lasts 10 min.
     * Without checkpointing, the task restarts from scratch at min 27 and finishes at min 37.
     */
    @Test
    fun `two failures each restart the task from scratch`() {
        val failureModel =
            InlineTraceBasedFailureSpec(
                listOf(
                    FailureSpec(TimeDelta.ofMin(5), TimeDelta.ofMin(5), 1.0),
                    FailureSpec(TimeDelta.ofMin(7), TimeDelta.ofMin(10), 1.0),
                ),
                repeat = false,
            )

        val monitor = runTest(topology, listOf(tenMinuteTask()), failureModel)

        // The task waits 5 and 10 min for the host to recover, and loses the 5 and 7 min of work before each failure
        val expectedSchedulingDelay = (5 + 10) * MINUTE
        val expectedFailureDelay = (5 + 7) * MINUTE

        // The task runs for its 10 min plus the work it redoes. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay
        val runtime = runningTime + expectedSchedulingDelay
        val activeTime = runningTime / 2
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(runtime - activeTime, monitor.hostCpuIdleTimes.getValue(HOST).last()) { "Idle time incorrect" } },
            { assertEquals(activeTime, monitor.hostCpuActiveTimes.getValue(HOST).last()) { "Active time incorrect" } },
            { assertEquals(HALF_LOAD_POWER, monitor.hostPowerDraws.getValue(HOST)[0]) { "Power draw at sample 0 incorrect" } },
            { assertEquals(IDLE_POWER, monitor.hostPowerDraws.getValue(HOST)[5]) { "Power draw at sample 5 incorrect" } },
            { assertEquals(HALF_LOAD_POWER, monitor.hostPowerDraws.getValue(HOST)[10]) { "Power draw at sample 10 incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
        )
    }

    /**
     * A single 10 min task runs with a repeating failure trace: the host fails after every 5 min of uptime, for 5 min.
     * The task never runs long enough to finish, and is terminated at its 10th failure, the default `maxNumFailures`
     * of a scenario.
     */
    @Test
    fun `a task is terminated after ten failures`() {
        val monitor = runTest(topology, listOf(tenMinuteTask()), repeatedFailures)

        // The task waits 5 min for the host to recover after every failure but the last, when it is terminated.
        // All work of its 10 runs of 5 min is lost.
        val expectedSchedulingDelay = 9 * 5 * MINUTE
        val expectedFailureDelay = 10 * 5 * MINUTE

        // The task never finishes, so all the time it runs is lost work
        val runningTime = expectedFailureDelay
        val runtime = runningTime + expectedSchedulingDelay
        val activeTime = runningTime / 2
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        // The host alternates between running the task for 5 min and being failed for 5 min
        val powerDrawAssertions =
            ((0..90 step 5) + 94).map { sample ->
                {
                    val expected = if ((sample / 5) % 2 == 0) HALF_LOAD_POWER else IDLE_POWER
                    assertEquals(expected, monitor.hostPowerDraws.getValue(HOST)[sample]) { "Power draw at sample $sample incorrect" }
                }
            }

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(runtime - activeTime, monitor.hostCpuIdleTimes.getValue(HOST).last()) { "Idle time incorrect" } },
            { assertEquals(activeTime, monitor.hostCpuActiveTimes.getValue(HOST).last()) { "Active time incorrect" } },
            { assertAll("Power draws of $HOST", powerDrawAssertions) },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            { assertEquals(1, monitor.tasksTerminated) { "Number of terminated tasks incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
        )
    }

    /**
     * A custom failure model injects a 7.5 min failure 15 min after the previous one ended, so the host is failed from
     * min 15 to 22.5 and from min 37.5 to 45.
     * The first task finishes at min 10, before the first failure.
     * The second task is submitted at min 30 and is interrupted by the second failure after running 7.5 min.
     * Without checkpointing, it restarts from scratch when the host recovers at min 45, and finishes at min 55.
     */
    @Test
    fun `a custom failure model keeps injecting failures`() {
        val workload = listOf(tenMinuteTask(id = 0), tenMinuteTask(id = 1, submissionTime = "1970-01-01T00:30"))

        // Distributions are sampled in hours
        val failureModel =
            CustomFailureSpec(
                interArrival = ConstantDistributionSpec(0.25),
                duration = ConstantDistributionSpec(0.125),
                hostFraction = ConstantDistributionSpec(1.0),
            )

        val monitor = runTest(topology, workload, failureModel)

        // Task 1 is interrupted after 7.5 min, and then waits 7.5 min for the host to recover
        val expectedSchedulingDelay1 = 15 * MINUTE / 2
        val expectedFailureDelay1 = 15 * MINUTE / 2

        // Both tasks run for their 10 min, and task 1 also redoes its lost work
        val runningTime = 10 * MINUTE + 10 * MINUTE + expectedFailureDelay1
        val runtime = 30 * MINUTE + expectedFailureDelay1 + expectedSchedulingDelay1 + 10 * MINUTE
        val activeTime = runningTime / 2

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(runtime - activeTime, monitor.hostCpuIdleTimes.getValue(HOST).last()) { "Idle time incorrect" } },
            { assertEquals(activeTime, monitor.hostCpuActiveTimes.getValue(HOST).last()) { "Active time incorrect" } },
            // Task 0 finishes before the first failure, so it has no delays
            { assertDelays(monitor, taskId = 0, scheduling = 0, failure = 0, checkpoint = 0) },
            { assertDelays(monitor, taskId = 1, scheduling = expectedSchedulingDelay1, failure = expectedFailureDelay1, checkpoint = 0) },
        )
    }

    /**
     * A single 10 min task makes a 1 s snapshot after every minute of work, without any failures.
     * A snapshot is made after minutes 1 to 9 of work, but not at the end of the task, which adds 9 s to the runtime.
     */
    @Test
    fun `checkpointing without failures only adds the snapshot time`() {
        val monitor = runTest(topology, listOf(tenMinuteTask()), checkpointModel = checkpointEveryMinute)

        val expectedCheckpointDelay = 9 * SECOND

        val runtime = 10 * MINUTE + expectedCheckpointDelay
        // Snapshots run at the task's peak demand, so the host runs at half load throughout
        val energyUsage = energy(HALF_LOAD_POWER, runtime)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = 0, failure = 0, checkpoint = expectedCheckpointDelay) },
        )
    }

    /**
     * A single 10 min task makes a 1 s snapshot after every minute of work, and is interrupted by a failure after 5 min.
     * The last snapshot before the failure covers 4 min of work and finishes at 244 s, so only the 56 s of work since
     * then is lost.
     * When the host recovers after 5 min, the task resumes from that snapshot with 6 min of work left.
     * In total, 9 snapshots are made: 4 before the failure and 5 after.
     */
    @Test
    fun `a checkpointed task only redoes the work since its last snapshot`() {
        val monitor = runTest(topology, listOf(tenMinuteTask()), singleFailure, checkpointModel = checkpointEveryMinute)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 56 * SECOND
        val expectedCheckpointDelay = 9 * SECOND

        // The task runs for its 10 min plus the work it redoes and its snapshots. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay + expectedCheckpointDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /**
     * A single 10 min task makes a 50 s snapshot after every minute of work, so its snapshots run from 60 to 110 s, from
     * 170 to 220 s and from 280 to 330 s. A failure after 5 min interrupts the third snapshot.
     *
     * The interrupted snapshot is incomplete, so the task should resume from the second snapshot, which covers 2 min of
     * work, and lose the 1 min of work done since then.
     * The 20 s spent on the interrupted snapshot counts as checkpoint delay, like the time of the completed snapshots.
     * When the host recovers after 5 min, the task has 8 min of work left, for which it makes 7 more snapshots.
     *
     * TODO: OpenDC does not return the correct values. This will be solved later, and has been marked as a bug
     */
    @Test @Disabled
    fun `a failure during a snapshot falls back to the previous snapshot`() {
        val checkpointModel = CheckpointModelSpec(interval = TimeDelta.ofMin(1), duration = TimeDelta.ofSec(50))

        val monitor = runTest(topology, listOf(tenMinuteTask()), singleFailure, checkpointModel = checkpointModel)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 1 * MINUTE
        // Two complete snapshots and 20 s of the interrupted one before the failure, and 7 snapshots after it
        val expectedCheckpointDelay = 2 * 50 * SECOND + 20 * SECOND + 7 * 50 * SECOND

        // The task runs for its 10 min plus the work it redoes and its snapshots. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay + expectedCheckpointDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /**
     * A 20 min task runs 10 min at 2000 MHz followed by 10 min at 1000 MHz.
     * It makes a 1 s snapshot after every minute of work, and is interrupted by a failure after 5 min.
     * As with the 10 min task, 56 s of work is lost and the task resumes from the snapshot at 4 min of work.
     * In total, 19 snapshots are made.
     *
     * Snapshots run at the task's peak demand (2000 MHz), regardless of the fragment that is running.
     * The failure hits the 2000 MHz fragment, so the lost work also ran at full load.
     */
    @Test
    fun `snapshots run at peak demand when the failure hits the high-demand fragment`() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * MINUTE, 2000.0), fragment(10 * MINUTE, 1000.0))),
            )

        val monitor = runTest(topology, workload, singleFailure, checkpointModel = checkpointEveryMinute)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 56 * SECOND
        val expectedCheckpointDelay = 19 * SECOND

        val runtime = 20 * MINUTE + expectedFailureDelay + expectedCheckpointDelay + expectedSchedulingDelay
        val energyUsage =
            energy(FULL_LOAD_POWER, 10 * MINUTE + expectedFailureDelay + expectedCheckpointDelay) +
                energy(HALF_LOAD_POWER, 10 * MINUTE) +
                energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /**
     * Like the high-demand variant, but with the fragments reversed: 10 min at 1000 MHz followed by 10 min at 2000 MHz.
     * The failure now hits the 1000 MHz fragment, so the lost work ran at half load, while the snapshots still run at
     * the task's peak demand (2000 MHz).
     */
    @Test
    fun `snapshots run at peak demand when the failure hits the low-demand fragment`() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * MINUTE, 1000.0), fragment(10 * MINUTE, 2000.0))),
            )

        val monitor = runTest(topology, workload, singleFailure, checkpointModel = checkpointEveryMinute)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 56 * SECOND
        val expectedCheckpointDelay = 19 * SECOND

        val runtime = 20 * MINUTE + expectedFailureDelay + expectedCheckpointDelay + expectedSchedulingDelay
        val energyUsage =
            energy(FULL_LOAD_POWER, 10 * MINUTE + expectedCheckpointDelay) +
                energy(HALF_LOAD_POWER, 10 * MINUTE + expectedFailureDelay) +
                energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /**
     * A single 10 min task makes a 1 s snapshot after every minute of work, with a repeating failure trace: the host
     * fails after every 5 min of uptime, for 5 min.
     * At each failure, 4 more minutes of work are saved and only the 56 s since the last snapshot is lost.
     * After the second failure, 8 min of work is saved, and the task finishes before the third failure.
     * In total, 9 snapshots are made: 4 in each of the first two runs and 1 in the last.
     *
     * Without checkpointing, this task would be terminated instead (see `a task is terminated after ten failures`).
     */
    @Test
    fun `checkpointing lets a task finish despite repeated failures`() {
        val monitor = runTest(topology, listOf(tenMinuteTask()), repeatedFailures, checkpointModel = checkpointEveryMinute)

        val expectedSchedulingDelay = 2 * 5 * MINUTE
        val expectedFailureDelay = 2 * 56 * SECOND
        val expectedCheckpointDelay = (4 + 4 + 1) * SECOND

        // The task runs for its 10 min plus the work it redoes and its snapshots. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay + expectedCheckpointDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /**
     * A 10 min task consists of fragments of 2.5, 2 and 5.5 min. It makes a 1 s snapshot after every minute of work, with
     * a repeating failure trace: the host fails after every 5 min of uptime, for 5 min.
     *
     * The last snapshot before the first failure is made after 4 min of work, in the second fragment. The failure hits
     * 26 s into the third fragment, so the 56 s of work lost since the snapshot spans two fragments.
     * The task then resumes from that snapshot in the second fragment, and the second failure hits the third fragment.
     * Apart from the fragments, this is the same timeline as `checkpointing lets a task finish despite repeated failures`.
     */
    @Test
    fun `a task resumes from a snapshot in a later fragment after repeated failures`() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments = listOf(fragment(150 * SECOND, 1000.0), fragment(2 * MINUTE, 1000.0), fragment(330 * SECOND, 1000.0)),
                ),
            )

        val monitor = runTest(topology, workload, repeatedFailures, checkpointModel = checkpointEveryMinute)

        val expectedSchedulingDelay = 2 * 5 * MINUTE
        val expectedFailureDelay = 2 * 56 * SECOND
        val expectedCheckpointDelay = (4 + 4 + 1) * SECOND

        // The task runs for its 10 min plus the work it redoes and its snapshots. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay + expectedCheckpointDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /**
     * A single 10 min task makes 1 s snapshots with an interval that starts at 1 min and grows by a factor 1.5 after
     * every snapshot. It is interrupted by a failure after 5 min.
     *
     * Before the failure, snapshots are made after 1, 2.5 and 4.75 min of work. The last one finishes at 288 s, so
     * only the 12 s of work since then is lost.
     * After the failure, the interval restarts at 1 min, so the remaining 5.25 min of work again gets snapshots after
     * 1, 2.5 and 4.75 min. In total, 6 snapshots are made.
     */
    @Test
    fun `a scaling checkpoint interval restarts from its initial value after a failure`() {
        val monitor =
            runTest(
                topology,
                listOf(tenMinuteTask()),
                singleFailure,
                checkpointModel = checkpointEveryMinute.copy(intervalScaling = 1.5),
            )

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 12 * SECOND
        val expectedCheckpointDelay = (3 + 3) * SECOND

        // The task runs for its 10 min plus the work it redoes and its snapshots. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay + expectedCheckpointDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.hostEnergyUsages.getValue(HOST).last()) { "Total energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /**
     * The delays of a task should be reported from the moment they occur, not only when the task finishes.
     *
     * A single 10 min task makes a 1 s snapshot after every 50 s of work, and is interrupted by a failure after 5.5 min,
     * for 5 min. These times make sure that no snapshot, failure or recovery coincides with a sample.
     * The last snapshot before the failure starts at 305 s and covers 5 min of work, so 24 s of work is lost.
     *
     * The task is sampled every minute while it runs: at min 1 to 5 before the failure, and at min 11 to 15 after it
     * restarts at 630 s. It is sampled once more when it finishes at 935 s.
     */
    @Test
    fun `delays are reported from the moment they occur`() {
        val failureModel = InlineTraceBasedFailureSpec(listOf(FailureSpec(TimeDelta.ofSec(330), TimeDelta.ofMin(5), 1.0)), repeat = false)
        val checkpointModel = CheckpointModelSpec(interval = TimeDelta.ofSec(50), duration = TimeDelta.ofSec(1))

        val monitor = runTest(topology, listOf(tenMinuteTask()), failureModel, checkpointModel = checkpointModel)

        // The scheduling and failure delays are known once the task restarts, and do not change after that
        val expectedSchedulingDelays = List(5) { 0L } + List(6) { 5 * MINUTE }
        val expectedFailureDelays = List(5) { 0L } + List(6) { 24 * SECOND }
        // The checkpoint delay counts the snapshots started so far: at 50, 101, 152, 203, 254 and 305 s before the
        // failure, and at 680, 731, 782, 833 and 884 s after it
        val expectedCheckpointDelays = listOf(1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 11).map { it * SECOND }

        assertAll(
            { assertEquals(expectedSchedulingDelays, monitor.taskSchedulingDelay.getValue(0)) { "Scheduling delays incorrect" } },
            { assertEquals(expectedFailureDelays, monitor.taskFailureDelay.getValue(0)) { "Failure delays incorrect" } },
            { assertEquals(expectedCheckpointDelays, monitor.taskCheckpointDelay.getValue(0)) { "Checkpoint delays incorrect" } },
        )
    }

    /**
     * A single-core task that runs at 1000 MHz for 10 min.
     * This is half of the host's capacity, so the host's CPU is active for half of the time the task runs.
     */
    private fun tenMinuteTask(
        id: Int = 0,
        submissionTime: String = "1970-01-01T00:00",
    ): TaskSpec = createTestTask(id = id, submissionTime = submissionTime, fragments = listOf(fragment(10 * MINUTE, 1000.0)))
}

/*
 * Copyright (c) 2026 AtLarge Research
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.TempDir
import org.opendc.sdk.model.scheduler.ComputeHostFilterSpec
import org.opendc.sdk.model.scheduler.CoreRamWeigherSpec
import org.opendc.sdk.model.scheduler.FilterAllocationPolicySpec
import org.opendc.sdk.model.scheduler.RamFilterSpec
import org.opendc.sdk.model.scheduler.TaskOrderSpec
import org.opendc.sdk.model.scheduler.VCpuFilterSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.createTopology
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * Tests for placing waiting tasks by priority instead of first in, first out.
 */
class TaskOrderTest {
    private val tenMinutes = 10 * 60 * 1000L

    /**
     * On a host with two cores: tasks 0 and 1 have no children, task 2 has child 3. Tasks 0, 1 and 2 arrive together,
     * in that order. First in, first out runs 0 and 1, then 2, then 3: 30 minutes. Running 2 first lets 3 overlap
     * with 0 and 1: 20 minutes.
     */
    private fun workload(): List<TaskSpec> =
        listOf(
            createTestTask(id = 0, duration = tenMinutes, fragments = listOf(fragment(tenMinutes, 1000.0))),
            createTestTask(id = 1, duration = tenMinutes, fragments = listOf(fragment(tenMinutes, 1000.0))),
            createTestTask(id = 2, duration = tenMinutes, fragments = listOf(fragment(tenMinutes, 1000.0)), children = setOf(3)),
            createTestTask(id = 3, duration = tenMinutes, fragments = listOf(fragment(tenMinutes, 1000.0)), parents = setOf(2)),
        )

    private fun makespan(order: TaskOrderSpec?): Long {
        val policy =
            FilterAllocationPolicySpec(
                filters = listOf(ComputeHostFilterSpec, VCpuFilterSpec(1.0), RamFilterSpec(1.0)),
                weighers = listOf(CoreRamWeigherSpec(1.0)),
                taskOrder = order,
            )
        return runTest(createTopology("single_2_2000.json"), workload(), allocationPolicy = policy).maxTimestamp
    }

    @Test
    fun testTaskOrder(
        @TempDir dir: Path,
    ) {
        val first = dir.resolve("first.csv")
        first.writeText("task_id,score\n2,1.0\n")
        val equal = dir.resolve("equal.csv")
        equal.writeText("task_id,score\n0,1.0\n1,1.0\n2,1.0\n3,1.0\n")

        assertAll(
            { assertEquals(3 * tenMinutes, makespan(null)) { "First in, first out" } },
            { assertEquals(2 * tenMinutes, makespan(TaskOrderSpec(first.toString()))) { "Task 2 first" } },
            { assertEquals(3 * tenMinutes, makespan(TaskOrderSpec(equal.toString()))) { "Equal priorities keep arrival order" } },
        )
    }
}

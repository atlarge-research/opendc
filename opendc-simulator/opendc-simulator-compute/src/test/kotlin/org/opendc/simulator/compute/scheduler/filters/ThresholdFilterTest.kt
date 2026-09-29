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

package org.opendc.simulator.compute.scheduler.filters

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.models.GpuHostModel
import org.opendc.simulator.compute.models.HostModel
import org.opendc.simulator.compute.task.SimTask
import java.util.Random

/**
 * Checks the contract of every [ThresholdFilter]: a host only passes when its available room covers the required room.
 */
internal class ThresholdFilterTest {
    /** The state of the mocked host and task, drawn anew for every check. */
    private class State(random: Random) {
        val cores = 1 + random.nextInt(32)
        val provisionedCores = random.nextInt(2 * cores)
        val memory = 1024L * (1 + random.nextInt(64))
        val freeMemory = memory - random.nextLong(memory + memory / 2)
        val instances = random.nextInt(8)
        val gpus = random.nextInt(3)
        val gpuCores = 1 + random.nextInt(8)
        val provisionedGpuCores = random.nextInt(2 * gpus * gpuCores + 1)
        val coreSpeed = 1000.0 + random.nextInt(3000)

        val taskCores = 1 + random.nextInt(16)
        val taskMemory = 512 * (1 + random.nextInt(64))
        val taskGpuCores = random.nextInt(8)
        val taskCoreSpeed = 1000.0 + random.nextInt(3000)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filters")
    fun testPassingHostHasEnoughRoom(filter: ThresholdFilter) {
        val random = Random(0)
        var state = State(random)

        val host = mockk<SimHost>()
        every { host.model } answers {
            HostModel(
                state.cores * state.coreSpeed,
                state.cores,
                state.memory,
                List(state.gpus) {
                    GpuHostModel(state.gpuCores * 1000.0, state.gpuCores, 0L, 0.0)
                },
            )
        }
        every { host.provisionedCpuCores } answers { state.provisionedCores }
        every { host.availableCpuCores } answers { state.cores - state.provisionedCores }
        every { host.availableMemory } answers { state.freeMemory }
        every { host.instanceCount } answers { state.instances }
        every { host.provisionedGpuCores } answers { state.provisionedGpuCores }

        val task = mockk<SimTask>()
        every { task.cpuCoreCount } answers { state.taskCores }
        every { task.memorySize } answers { state.taskMemory }
        every { task.gpuCoreCount } answers { state.taskGpuCores }
        every { task.cpuCapacity } answers { state.taskCores * state.taskCoreSpeed }

        var passed = 0
        repeat(500) {
            state = State(random)
            if (filter.test(host, task)) {
                passed++
                val available = filter.available(host)
                val required = filter.required(task)
                assertTrue(available >= required) { "$filter passed a host with room $available for a task needing $required" }
            }
        }

        // Make sure the check was not vacuous
        assertTrue(passed in 1..499) { "$filter passed $passed of 500 hosts" }
    }

    companion object {
        @JvmStatic
        fun filters(): List<ThresholdFilter> =
            listOf(
                VCpuFilter(1.0),
                VCpuFilter(2.5),
                RamFilter(1.0),
                RamFilter(1.5),
                VGpuFilter(1.0),
                VGpuFilter(2.0),
                InstanceCountFilter(4),
                VCpuCapacityFilter(),
            )
    }
}

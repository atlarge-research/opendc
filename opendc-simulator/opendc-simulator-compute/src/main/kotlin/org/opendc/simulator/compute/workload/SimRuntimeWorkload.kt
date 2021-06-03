/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.simulator.compute.workload

import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.util.SimWorkloadLifecycle
import org.opendc.simulator.resources.consumer.SimWorkConsumer

/**
 * A [SimWorkload] that models application execution as a single duration.
 *
 * @property duration The duration of the workload.
 * @property utilization The utilization of the application during runtime.
 */
public class SimRuntimeWorkload(
    public val duration: Long,
    public val utilization: Double = 0.8
) : SimWorkload {
    init {
        require(duration >= 0) { "Duration must be non-negative" }
        require(utilization > 0.0 && utilization <= 1.0) { "Utilization must be in (0, 1]" }
    }

    override fun onStart(ctx: SimMachineContext) {
        val lifecycle = SimWorkloadLifecycle(ctx)
        for (cpu in ctx.cpus) {
            val limit = cpu.capacity * utilization
            cpu.startConsumer(lifecycle.waitFor(SimWorkConsumer((limit / 1000) * duration, utilization)))
        }
    }

    override fun toString(): String = "SimRuntimeWorkload(duration=$duration,utilization=$utilization)"
}

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
import org.opendc.simulator.compute.model.SimProcessingUnit
import org.opendc.simulator.resources.SimResourceCommand
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext

/**
 * A [SimWorkload] that models applications as a static number of floating point operations ([flops]) executed on
 * multiple cores of a compute resource.
 *
 * @property flops The number of floating point operations to perform for this task in MFLOPs.
 * @property utilization A model of the CPU utilization of the application.
 */
public class SimFlopsWorkload(
    public val flops: Long,
    public val utilization: Double = 0.8
) : SimWorkload {
    init {
        require(flops >= 0) { "Number of FLOPs must be positive" }
        require(utilization > 0.0 && utilization <= 1.0) { "Utilization must be in (0, 1]" }
    }

    override fun onStart(ctx: SimMachineContext) {}

    override fun getConsumer(ctx: SimMachineContext, cpu: SimProcessingUnit): SimResourceConsumer<SimProcessingUnit> {
        return CpuConsumer(ctx)
    }

    private inner class CpuConsumer(private val machine: SimMachineContext) : SimResourceConsumer<SimProcessingUnit> {
        override fun onStart(ctx: SimResourceContext<SimProcessingUnit>): SimResourceCommand {
            val limit = ctx.resource.frequency * utilization
            val work = flops.toDouble() / machine.cpus.size

            return if (work > 0.0) {
                SimResourceCommand.Consume(work, limit)
            } else {
                SimResourceCommand.Exit
            }
        }

        override fun onNext(ctx: SimResourceContext<SimProcessingUnit>, remainingWork: Double): SimResourceCommand {
            return if (remainingWork > 0.0) {
                val limit = ctx.resource.frequency * utilization
                return SimResourceCommand.Consume(remainingWork, limit)
            } else {
                SimResourceCommand.Exit
            }
        }
    }

    override fun toString(): String = "SimFlopsWorkload(FLOPs=$flops,utilization=$utilization)"
}

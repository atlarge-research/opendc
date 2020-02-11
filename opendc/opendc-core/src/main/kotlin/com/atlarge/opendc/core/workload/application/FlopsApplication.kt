/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.core.workload.application

import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.ReceiveRef
import com.atlarge.odcsim.sendOnce
import com.atlarge.opendc.core.User
import com.atlarge.opendc.core.resources.compute.ProcessingElement
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

/**
 * An [Application] implementation that models applications performing a static number of floating point operations
 * ([flops]) on a compute resource.
 *
 * @property uid A unique identifier for this application.
 * @property name The name of the application.
 * @property owner The owner of this application.
 * @property cores The number of cores needed for this application.
 * @property flops The number of floating point operations to perform for this task.
 */
class FlopsApplication(
    override val uid: UUID,
    override val name: String,
    override val owner: User,
    override val cores: Int,
    val flops: Long
) : Application {

    init {
        require(flops >= 0) { "Negative number of flops" }
    }

    /**
     * Build the runtime [Behavior] based on a number of floating point operations to execute.
     */
    override suspend fun invoke(ctx: ProcessContext, pid: Pid, main: ReceiveRef<ProcessMessage>) {
        val inlet = ctx.listen(main)
        var remaining = flops
        var start: Long = 0
        lateinit var allocation: Map<ProcessingElement, Double>

        val created = inlet.receive() as ProcessMessage.Setup
        val ref = created.ref

        ref.sendOnce(ProcessEvent.Ready(pid))

        suspend fun processAllocation(resources: Map<ProcessingElement, Double>, until: Long) {
            start = ctx.clock.millis()
            allocation = resources
                .asSequence()
                .take(cores)
                .associateBy({ it.key }, { it.value })

            val speed = allocation.asSequence()
                .map { (key, value) -> key.unit.clockRate * value }
                .average()
            val finishedAt = ceil(ctx.clock.millis() + remaining / speed).toLong()
            ref.sendOnce(ProcessEvent.Consume(pid, allocation, min(finishedAt, until)))
        }

        var msg = inlet.receive() as ProcessMessage.Allocation
        processAllocation(msg.resources, msg.until)

        coroutineScope {
            while (isActive) {
                msg = inlet.receive() as ProcessMessage.Allocation

                /* Compute the consumption of flops */
                val consumed = allocation.asSequence()
                    .map { (key, value) -> key.unit.clockRate * value * (ctx.clock.millis() - start) }
                    .sum()
                // Ceil to prevent consumed flops being rounded to 0
                remaining -= ceil(consumed).toLong()

                /* Test whether all flops have been consumed and the task is finished */
                if (remaining <= 0) {
                    ref.sendOnce(ProcessEvent.Exit(pid, 0))
                    break
                }
                processAllocation(msg.resources, msg.until)
            }
        }

        inlet.close()
    }
}

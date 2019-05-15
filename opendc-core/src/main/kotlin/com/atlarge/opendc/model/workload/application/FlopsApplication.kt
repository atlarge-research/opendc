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

package com.atlarge.opendc.model.workload.application

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.DeferredBehavior
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.receive
import com.atlarge.odcsim.stopped
import com.atlarge.odcsim.unhandled
import com.atlarge.opendc.model.User
import com.atlarge.opendc.model.resources.compute.Machine
import com.atlarge.opendc.model.resources.compute.ProcessingElement
import java.lang.Double.min
import java.util.UUID
import kotlin.math.ceil

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
    override fun invoke(): Behavior<ProcessMessage> = object : DeferredBehavior<ProcessMessage>() {
        /**
         * The remaining number of floating point operations to execute.
         */
        var remaining = flops

        /**
         * The machine to which the task is assigned.
         */
        lateinit var machine: Machine

        /**
         * The reference to the machine instance.
         */
        lateinit var ref: ActorRef<ProcessEvent>

        /**
         * The start of the last allocation
         */
        var start: Instant = 0.0

        /**
         * The given resource allocation.
         */
        lateinit var allocation: Map<ProcessingElement, Double>

        override fun invoke(ctx: ActorContext<ProcessMessage>) = created()

        /**
         * Handle the initial, created state of a task instance.
         */
        private fun created(): Behavior<ProcessMessage> = receive { ctx, msg ->
            when (msg) {
                is ProcessMessage.Setup -> {
                    machine = msg.machine
                    ref = msg.ref
                    /* TODO implement setup time */
                    ctx.send(ref, ProcessEvent.Ready(ctx.self))
                    ready().narrow()
                }
                else -> unhandled()
            }
        }

        /**
         * Handle the ready state of a task instance.
         */
        private fun ready(): Behavior<Any> = receive { ctx, msg ->
            when (msg) {
                is ProcessMessage.Allocation -> {
                    processAllocation(ctx, msg.resources, msg.until)
                    running()
                }
                else -> unhandled()
            }
        }

        /**
         * Handle the running state of a task instance.
         */
        private fun running(): Behavior<Any> = receive { ctx, msg ->
            when (msg) {
                is ProcessMessage.Allocation -> {
                    /* Compute the consumption of flops */
                    val consumed = allocation.asSequence()
                        .map { (key, value) -> key.unit.clockRate * value * (ctx.time - start) }
                        .sum()
                    // Ceil to prevent consumed flops being rounded to 0
                    remaining -= ceil(consumed).toLong()

                    /* Test whether all flops have been consumed and the task is finished */
                    if (remaining <= 0) {
                        ctx.send(ref, ProcessEvent.Exit(ctx.self, 0))
                        return@receive stopped()
                    }

                    processAllocation(ctx, msg.resources, msg.until)
                    running().narrow()
                }
                else -> unhandled()
            }
        }

        private fun processAllocation(ctx: ActorContext<Any>, resources: Map<ProcessingElement, Double>, until: Instant) {
            start = ctx.time
            allocation = resources
                .asSequence()
                .take(cores)
                .associateBy({ it.key }, { it.value })

            val speed = allocation.asSequence()
                .map { (key, value) -> key.unit.clockRate * value }
                .average()
            val finishedAt = ctx.time + remaining / speed
            ctx.send(ref, ProcessEvent.Consume(ctx.self, allocation, min(finishedAt, until)))
        }
    }
}

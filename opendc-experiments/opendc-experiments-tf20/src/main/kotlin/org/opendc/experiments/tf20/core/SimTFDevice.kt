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

package org.opendc.experiments.tf20.core

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.common.Labels
import kotlinx.coroutines.*
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.cpufreq.PerformanceScalingGovernor
import org.opendc.simulator.compute.cpufreq.SimpleScalingDriver
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.SimResourceCommand
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext
import org.opendc.simulator.resources.SimResourceEvent
import java.time.Clock
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * A [TFDevice] implementation using simulated components.
 */
public class SimTFDevice(
    override val uid: UUID,
    override val isGpu: Boolean,
    context: CoroutineContext,
    clock: Clock,
    meter: Meter,
    private val pu: ProcessingUnit,
    private val memory: MemoryUnit,
    powerModel: PowerModel
) : TFDevice {
    /**
     * The scope in which the device runs.
     */
    private val scope = CoroutineScope(context + Job())

    /**
     * The [SimMachine] representing the device.
     */
    private val machine = SimBareMetalMachine(
        scope.coroutineContext, clock, SimMachineModel(listOf(pu), listOf(memory)),
        PerformanceScalingGovernor(), SimpleScalingDriver(powerModel)
    )

    /**
     * The usage of the device.
     */
    private val _usage = meter.doubleValueRecorderBuilder("device.usage")
        .setDescription("The amount of device resources used")
        .setUnit("MHz")
        .build()
        .bind(Labels.of("device", uid.toString()))

    /**
     * The power draw of the device.
     */
    private val _power = meter.doubleValueRecorderBuilder("device.power")
        .setDescription("The power draw of the device")
        .setUnit("W")
        .build()
        .bind(Labels.of("device", uid.toString()))

    /**
     * The workload that will be run by the device.
     */
    private val workload = object : SimWorkload, SimResourceConsumer {
        /**
         * The resource context to interrupt the workload with.
         */
        var ctx: SimResourceContext? = null

        /**
         * The capacity of the device.
         */
        private var capacity: Double = 0.0

        /**
         * The queue of work to run.
         */
        val queue = ArrayDeque<Work>()

        /**
         * A flag to indicate that the workload is idle.
         */
        val isIdle
            get() = activeWork == null

        /**
         * The active work of the workload.
         */
        private var activeWork: Work? = null

        override fun onStart(ctx: SimMachineContext) {}

        override fun getConsumer(ctx: SimMachineContext, cpu: ProcessingUnit): SimResourceConsumer = this

        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            val activeWork = activeWork
            if (activeWork != null) {
                if (activeWork.consume(activeWork.flops - ctx.remainingWork)) {
                    this.activeWork = null
                } else {
                    return SimResourceCommand.Consume(activeWork.flops, ctx.capacity)
                }
            }

            val queue = queue
            val head = queue.poll()
            return if (head != null) {
                this.activeWork = head
                SimResourceCommand.Consume(head.flops, ctx.capacity)
            } else {
                SimResourceCommand.Idle()
            }
        }

        override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
            when (event) {
                SimResourceEvent.Start -> {
                    this.ctx = ctx
                    this.capacity = ctx.capacity
                }
                SimResourceEvent.Capacity -> {
                    this.capacity = ctx.capacity
                    ctx.interrupt()
                }
                SimResourceEvent.Run -> {
                    _usage.record(ctx.speed)
                    _power.record(machine.powerDraw)
                }
                else -> {}
            }
        }
    }

    init {
        scope.launch {
            machine.run(workload)
        }
    }

    override suspend fun load(dataSize: Long) {
        val duration = dataSize / memory.speed * 1000
        delay(duration.toLong())
    }

    override suspend fun compute(flops: Double) = suspendCancellableCoroutine<Unit> { cont ->
        workload.queue.add(Work(flops, cont))
        if (workload.isIdle) {
            workload.ctx?.interrupt()
        }
    }

    override fun close() {
        machine.close()
        scope.cancel()
    }

    private data class Work(var flops: Double, val cont: Continuation<Unit>) {
        fun consume(flops: Double): Boolean {
            this.flops -= flops

            if (this.flops <= 0) {
                cont.resume(Unit)
                return true
            }

            return false
        }
    }
}

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

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import kotlinx.coroutines.*
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.flow.*
import java.time.Clock
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.math.roundToLong

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
        FlowEngine(scope.coroutineContext, clock), MachineModel(listOf(pu), listOf(memory)),
        SimplePowerDriver(powerModel)
    )

    /**
     * The identifier of a device.
     */
    private val deviceId = AttributeKey.stringKey("device.id")

    /**
     * The usage of the device.
     */
    private val _usage = meter.histogramBuilder("device.usage")
        .setDescription("The amount of device resources used")
        .setUnit("MHz")
        .build()
        .bind(Attributes.of(deviceId, uid.toString()))

    /**
     * The power draw of the device.
     */
    private val _power = meter.histogramBuilder("device.power")
        .setDescription("The power draw of the device")
        .setUnit("W")
        .build()
        .bind(Attributes.of(deviceId, uid.toString()))

    /**
     * The workload that will be run by the device.
     */
    private val workload = object : SimWorkload, FlowSource {
        /**
         * The resource context to interrupt the workload with.
         */
        var ctx: FlowConnection? = null

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

        override fun onStart(ctx: SimMachineContext) {
            for (cpu in ctx.cpus) {
                cpu.startConsumer(this)
            }
        }

        override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
            val consumedWork = conn.rate * delta / 1000.0

            val activeWork = activeWork
            if (activeWork != null) {
                if (activeWork.consume(consumedWork)) {
                    this.activeWork = null
                } else {
                    val duration = (activeWork.flops / conn.capacity * 1000).roundToLong()
                    conn.push(conn.capacity)
                    return duration
                }
            }

            val queue = queue
            val head = queue.poll()
            return if (head != null) {
                this.activeWork = head
                val duration = (head.flops / conn.capacity * 1000).roundToLong()
                conn.push(conn.capacity)
                duration
            } else {
                conn.push(0.0)
                Long.MAX_VALUE
            }
        }

        override fun onEvent(conn: FlowConnection, now: Long, event: FlowEvent) {
            when (event) {
                FlowEvent.Start -> {
                    this.ctx = conn
                    this.capacity = conn.capacity
                }
                FlowEvent.Capacity -> {
                    this.capacity = conn.capacity
                    conn.pull()
                }
                FlowEvent.Converge -> {
                    _usage.record(conn.rate)
                    _power.record(machine.psu.powerDraw)
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
            workload.ctx?.pull()
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

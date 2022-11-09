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

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.opendc.common.Dispatcher
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.SimPsuFactories
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.CpuPowerModel
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.flow2.FlowStage
import org.opendc.simulator.flow2.FlowStageLogic
import org.opendc.simulator.flow2.OutPort
import java.util.ArrayDeque
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * A [TFDevice] implementation using simulated components.
 */
public class SimTFDevice(
    override val uid: UUID,
    override val isGpu: Boolean,
    dispatcher: Dispatcher,
    pu: ProcessingUnit,
    private val memory: MemoryUnit,
    powerModel: CpuPowerModel
) : TFDevice {
    /**
     * The [SimMachine] representing the device.
     */
    private val machine = SimBareMetalMachine.create(
        FlowEngine.create(dispatcher).newGraph(),
        MachineModel(listOf(pu), listOf(memory)),
        SimPsuFactories.simple(powerModel)
    )

    /**
     * The workload that will be run by the device.
     */
    private val workload = object : SimWorkload, FlowStageLogic {
        /**
         * The [FlowStage] of the workload.
         */
        var stage: FlowStage? = null

        /**
         * The output of the workload.
         */
        private var output: OutPort? = null

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

        /**
         * The timestamp of the last pull.
         */
        private var lastPull: Long = 0L

        override fun onStart(ctx: SimMachineContext) {
            val stage = ctx.graph.newStage(this)
            this.stage = stage
            output = stage.getOutlet("out")
            lastPull = ctx.graph.engine.clock.millis()

            ctx.graph.connect(output, ctx.cpus[0].input)
        }

        override fun onStop(ctx: SimMachineContext) {
            stage?.close()
            stage = null
            output = null
        }

        override fun snapshot(): SimWorkload = throw UnsupportedOperationException()

        override fun onUpdate(ctx: FlowStage, now: Long): Long {
            val output = output ?: return Long.MAX_VALUE
            val lastPull = lastPull
            this.lastPull = now
            val delta = (now - lastPull).coerceAtLeast(0)
            val consumedWork = output.rate * delta / 1000.0

            val activeWork = activeWork
            if (activeWork != null) {
                if (activeWork.consume(consumedWork)) {
                    this.activeWork = null
                } else {
                    val duration = ceil(activeWork.flops / output.capacity * 1000).toLong()
                    output.push(output.capacity)
                    return now + duration
                }
            }

            val queue = queue
            val head = queue.poll()
            return if (head != null) {
                this.activeWork = head
                val duration = (head.flops / output.capacity * 1000).roundToLong()
                output.push(output.capacity)
                now + duration
            } else {
                output.push(0.0f)
                Long.MAX_VALUE
            }
        }
    }

    init {
        machine.startWorkload(workload, emptyMap()) {}
    }

    override suspend fun load(dataSize: Long) {
        val duration = dataSize / memory.speed * 1000
        delay(duration.toLong())
    }

    override suspend fun compute(flops: Double) = suspendCancellableCoroutine<Unit> { cont ->
        workload.queue.add(Work(flops, cont))
        if (workload.isIdle) {
            workload.stage?.invalidate()
        }
    }

    override fun getDeviceStats(): TFDeviceStats {
        return TFDeviceStats(machine.cpuUsage, machine.psu.powerUsage, machine.psu.energyUsage)
    }

    override fun close() {
        machine.cancel()
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

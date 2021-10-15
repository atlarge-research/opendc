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

package org.opendc.simulator.compute

import kotlinx.coroutines.*
import org.opendc.simulator.compute.device.SimNetworkAdapter
import org.opendc.simulator.compute.device.SimPeripheral
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.NetworkAdapter
import org.opendc.simulator.compute.model.StorageDevice
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.flow.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Abstract implementation of the [SimMachine] interface.
 *
 * @param engine The engine to manage the machine's resources.
 * @param parent The parent simulation system.
 * @param model The model of the machine.
 */
public abstract class SimAbstractMachine(
    protected val engine: FlowEngine,
    private val parent: FlowConvergenceListener?,
    final override val model: MachineModel
) : SimMachine, FlowConvergenceListener {
    /**
     * The resources allocated for this machine.
     */
    public abstract val cpus: List<SimProcessingUnit>

    /**
     * The memory interface of the machine.
     */
    public val memory: SimMemory = Memory(FlowSink(engine, model.memory.sumOf { it.size }.toDouble()), model.memory)

    /**
     * The network interfaces available to the machine.
     */
    public val net: List<SimNetworkInterface> = model.net.mapIndexed { i, adapter -> NetworkAdapterImpl(engine, adapter, i) }

    /**
     * The network interfaces available to the machine.
     */
    public val storage: List<SimStorageInterface> = model.storage.mapIndexed { i, device -> StorageDeviceImpl(engine, device, i) }

    /**
     * The peripherals of the machine.
     */
    public override val peripherals: List<SimPeripheral> = net.map { it as SimNetworkAdapter }

    /**
     * A flag to indicate that the machine is terminated.
     */
    private var isTerminated = false

    /**
     * The current active [Context].
     */
    private var _ctx: Context? = null

    /**
     * This method is invoked when the machine is started.
     */
    protected open fun onStart(ctx: SimMachineContext) {}

    /**
     * This method is invoked when the machine is stopped.
     */
    protected open fun onStop(ctx: SimMachineContext) {
        _ctx = null
    }

    /**
     * Converge the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
     */
    override suspend fun run(workload: SimWorkload, meta: Map<String, Any>) {
        check(!isTerminated) { "Machine is terminated" }
        check(_ctx == null) { "A machine cannot run concurrently" }

        return suspendCancellableCoroutine { cont ->
            val ctx = Context(meta, cont)
            _ctx = ctx

            // Cancel all cpus on cancellation
            cont.invokeOnCancellation { ctx.close() }

            engine.batch {
                onStart(ctx)

                workload.onStart(ctx)
            }
        }
    }

    override fun close() {
        if (isTerminated) {
            return
        }

        isTerminated = true
        _ctx?.close()
    }

    override fun onConverge(now: Long, delta: Long) {
        parent?.onConverge(now, delta)
    }

    /**
     * The execution context in which the workload runs.
     */
    private inner class Context(override val meta: Map<String, Any>, private val cont: Continuation<Unit>) : SimMachineContext {
        /**
         * A flag to indicate that the context has been closed.
         */
        private var isClosed = false

        override val engine: FlowEngine
            get() = this@SimAbstractMachine.engine

        override val cpus: List<SimProcessingUnit> = this@SimAbstractMachine.cpus

        override val memory: SimMemory = this@SimAbstractMachine.memory

        override val net: List<SimNetworkInterface> = this@SimAbstractMachine.net

        override val storage: List<SimStorageInterface> = this@SimAbstractMachine.storage

        override fun close() {
            if (isClosed) {
                return
            }

            isClosed = true
            engine.batch {
                for (cpu in cpus) {
                    cpu.cancel()
                }
            }

            onStop(this)
            cont.resume(Unit)
        }
    }

    /**
     * The [SimMemory] implementation for a machine.
     */
    private class Memory(source: FlowSink, override val models: List<MemoryUnit>) : SimMemory, FlowConsumer by source {
        override fun toString(): String = "SimAbstractMachine.Memory"
    }

    /**
     * The [SimNetworkAdapter] implementation for a machine.
     */
    private class NetworkAdapterImpl(
        engine: FlowEngine,
        model: NetworkAdapter,
        index: Int
    ) : SimNetworkAdapter(), SimNetworkInterface {
        override val name: String = "eth$index"

        override val bandwidth: Double = model.bandwidth

        override val provider: FlowConsumer
            get() = _rx

        override fun createConsumer(): FlowSource = _tx

        override val tx: FlowConsumer
            get() = _tx
        private val _tx = FlowForwarder(engine)

        override val rx: FlowSource
            get() = _rx
        private val _rx = FlowForwarder(engine)

        override fun toString(): String = "SimAbstractMachine.NetworkAdapterImpl[name=$name,bandwidth=$bandwidth]"
    }

    /**
     * The [SimStorageInterface] implementation for a machine.
     */
    private class StorageDeviceImpl(
        engine: FlowEngine,
        model: StorageDevice,
        index: Int
    ) : SimStorageInterface {
        override val name: String = "disk$index"

        override val capacity: Double = model.capacity

        override val read: FlowConsumer = FlowSink(engine, model.readBandwidth)

        override val write: FlowConsumer = FlowSink(engine, model.writeBandwidth)

        override fun toString(): String = "SimAbstractMachine.StorageDeviceImpl[name=$name,capacity=$capacity]"
    }
}

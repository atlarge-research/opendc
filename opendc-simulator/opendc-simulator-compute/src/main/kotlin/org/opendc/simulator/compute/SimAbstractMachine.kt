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

import mu.KotlinLogging
import org.opendc.simulator.compute.device.SimNetworkAdapter
import org.opendc.simulator.compute.device.SimPeripheral
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.NetworkAdapter
import org.opendc.simulator.compute.model.StorageDevice
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.flow.FlowConsumer
import org.opendc.simulator.flow.FlowConvergenceListener
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.FlowForwarder
import org.opendc.simulator.flow.FlowSink
import org.opendc.simulator.flow.FlowSource
import org.opendc.simulator.flow.batch

/**
 * Abstract implementation of the [SimMachine] interface.
 *
 * @param engine The engine to manage the machine's resources.
 * @param model The model of the machine.
 */
public abstract class SimAbstractMachine(
    protected val engine: FlowEngine,
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
     * The current active [Context].
     */
    private var _ctx: Context? = null

    override fun startWorkload(workload: SimWorkload, meta: Map<String, Any>): SimMachineContext {
        check(_ctx == null) { "A machine cannot run concurrently" }

        val ctx = Context(workload, meta)
        ctx.start()
        return ctx
    }

    override fun cancel() {
        _ctx?.close()
    }

    override fun onConverge(now: Long) {}

    /**
     * The execution context in which the workload runs.
     *
     * @param workload The workload that is running on the machine.
     * @param meta The metadata passed to the workload.
     */
    private inner class Context(
        private val workload: SimWorkload,
        override val meta: Map<String, Any>
    ) : SimMachineContext {
        /**
         * A flag to indicate that the context has been closed.
         */
        private var isClosed = false

        val engine: FlowEngine = this@SimAbstractMachine.engine

        /**
         * Start this context.
         */
        fun start() {
            try {
                _ctx = this
                engine.batch { workload.onStart(this) }
            } catch (cause: Throwable) {
                logger.warn(cause) { "Workload failed during onStart callback" }
                close()
            }
        }

        override val cpus: List<SimProcessingUnit> = this@SimAbstractMachine.cpus

        override val memory: SimMemory = this@SimAbstractMachine.memory

        override val net: List<SimNetworkInterface> = this@SimAbstractMachine.net

        override val storage: List<SimStorageInterface> = this@SimAbstractMachine.storage

        override fun close() {
            if (isClosed) {
                return
            }

            isClosed = true
            assert(_ctx == this) { "Invariant violation: multiple contexts active for a single machine" }
            _ctx = null

            // Cancel all the resources associated with the machine
            doCancel()

            try {
                workload.onStop(this)
            } catch (cause: Throwable) {
                logger.warn(cause) { "Workload failed during onStop callback" }
            }
        }

        /**
         * Run the stop procedures for the resources associated with the machine.
         */
        private fun doCancel() {
            engine.batch {
                for (cpu in cpus) {
                    cpu.cancel()
                }

                memory.cancel()

                for (ifx in net) {
                    (ifx as NetworkAdapterImpl).disconnect()
                }

                for (storage in storage) {
                    val impl = storage as StorageDeviceImpl
                    impl.read.cancel()
                    impl.write.cancel()
                }
            }
        }

        override fun toString(): String = "SimAbstractMachine.Context"
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

    private companion object {
        /**
         * The logging instance associated with this class.
         */
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }
}

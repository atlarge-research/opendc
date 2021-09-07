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
import org.opendc.simulator.resources.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Abstract implementation of the [SimMachine] interface.
 *
 * @param interpreter The interpreter to manage the machine's resources.
 * @param parent The parent simulation system.
 * @param model The model of the machine.
 */
public abstract class SimAbstractMachine(
    protected val interpreter: SimResourceInterpreter,
    final override val parent: SimResourceSystem?,
    final override val model: MachineModel
) : SimMachine, SimResourceSystem {
    /**
     * The resources allocated for this machine.
     */
    public abstract val cpus: List<SimProcessingUnit>

    /**
     * The memory interface of the machine.
     */
    public val memory: SimMemory = Memory(SimResourceSource(model.memory.sumOf { it.size }.toDouble(), interpreter), model.memory)

    /**
     * The network interfaces available to the machine.
     */
    public val net: List<SimNetworkInterface> = model.net.mapIndexed { i, adapter -> NetworkAdapterImpl(adapter, i) }

    /**
     * The network interfaces available to the machine.
     */
    public val storage: List<SimStorageInterface> = model.storage.mapIndexed { i, device -> StorageDeviceImpl(interpreter, device, i) }

    /**
     * The peripherals of the machine.
     */
    public override val peripherals: List<SimPeripheral> = net.map { it as SimNetworkAdapter }

    /**
     * A flag to indicate that the machine is terminated.
     */
    private var isTerminated = false

    /**
     * The continuation to resume when the virtual machine workload has finished.
     */
    private var cont: Continuation<Unit>? = null

    /**
     * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
     */
    override suspend fun run(workload: SimWorkload, meta: Map<String, Any>) {
        check(!isTerminated) { "Machine is terminated" }
        check(cont == null) { "A machine cannot run concurrently" }

        val ctx = Context(meta)

        return suspendCancellableCoroutine { cont ->
            this.cont = cont

            // Cancel all cpus on cancellation
            cont.invokeOnCancellation {
                this.cont = null
                interpreter.batch {
                    for (cpu in cpus) {
                        cpu.cancel()
                    }
                }
            }

            interpreter.batch { workload.onStart(ctx) }
        }
    }

    override fun close() {
        if (isTerminated) {
            return
        }

        isTerminated = true
        cancel()
    }

    /**
     * Cancel the workload that is currently running on the machine.
     */
    private fun cancel() {
        interpreter.batch {
            for (cpu in cpus) {
                cpu.cancel()
            }
        }

        val cont = cont
        if (cont != null) {
            this.cont = null
            cont.resume(Unit)
        }
    }

    /**
     * The execution context in which the workload runs.
     */
    private inner class Context(override val meta: Map<String, Any>) : SimMachineContext {
        override val interpreter: SimResourceInterpreter
            get() = this@SimAbstractMachine.interpreter

        override val cpus: List<SimProcessingUnit> = this@SimAbstractMachine.cpus

        override val memory: SimMemory = this@SimAbstractMachine.memory

        override val net: List<SimNetworkInterface> = this@SimAbstractMachine.net

        override val storage: List<SimStorageInterface> = this@SimAbstractMachine.storage

        override fun close() = cancel()
    }

    /**
     * The [SimMemory] implementation for a machine.
     */
    private class Memory(source: SimResourceSource, override val models: List<MemoryUnit>) : SimMemory, SimResourceProvider by source {
        override fun toString(): String = "SimAbstractMachine.Memory"
    }

    /**
     * The [SimNetworkAdapter] implementation for a machine.
     */
    private class NetworkAdapterImpl(
        model: NetworkAdapter,
        index: Int
    ) : SimNetworkAdapter(), SimNetworkInterface {
        override val name: String = "eth$index"

        override val bandwidth: Double = model.bandwidth

        override val provider: SimResourceProvider
            get() = _rx

        override fun createConsumer(): SimResourceConsumer = _tx

        override val tx: SimResourceProvider
            get() = _tx
        private val _tx = SimResourceForwarder()

        override val rx: SimResourceConsumer
            get() = _rx
        private val _rx = SimResourceForwarder()

        override fun toString(): String = "SimAbstractMachine.NetworkAdapterImpl[name=$name,bandwidth=$bandwidth]"
    }

    /**
     * The [SimStorageInterface] implementation for a machine.
     */
    private class StorageDeviceImpl(
        interpreter: SimResourceInterpreter,
        model: StorageDevice,
        index: Int
    ) : SimStorageInterface {
        override val name: String = "disk$index"

        override val capacity: Double = model.capacity

        override val read: SimResourceProvider = SimResourceSource(model.readBandwidth, interpreter)

        override val write: SimResourceProvider = SimResourceSource(model.writeBandwidth, interpreter)

        override fun toString(): String = "SimAbstractMachine.StorageDeviceImpl[name=$name,capacity=$capacity]"
    }
}

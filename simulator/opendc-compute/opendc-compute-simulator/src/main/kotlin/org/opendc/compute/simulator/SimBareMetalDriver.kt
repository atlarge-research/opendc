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

package org.opendc.compute.simulator

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.opendc.compute.core.Flavor
import org.opendc.compute.core.image.Image
import org.opendc.compute.core.metal.Node
import org.opendc.compute.core.metal.NodeEvent
import org.opendc.compute.core.metal.NodeState
import org.opendc.compute.core.metal.driver.BareMetalDriver
import org.opendc.compute.simulator.power.api.CpuPowerModel
import org.opendc.compute.simulator.power.api.Powerable
import org.opendc.compute.simulator.power.models.ConstantPowerModel
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.failures.FailureDomain
import org.opendc.utils.flow.EventFlow
import org.opendc.utils.flow.StateFlow
import java.time.Clock
import java.util.UUID

/**
 * A basic implementation of the [BareMetalDriver] that simulates an [Image] running on a bare-metal machine.
 *
 * @param coroutineScope The [CoroutineScope] the driver runs in.
 * @param clock The virtual clock to keep track of time.
 * @param uid The unique identifier of the machine.
 * @param name An optional name of the machine.
 * @param metadata The initial metadata of the node.
 * @param machine The machine model to simulate.
 * @param cpuPowerModel The CPU power model of this machine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class SimBareMetalDriver(
    private val coroutineScope: CoroutineScope,
    private val clock: Clock,
    uid: UUID,
    name: String,
    metadata: Map<String, Any>,
    machine: SimMachineModel,
    cpuPowerModel: CpuPowerModel = ConstantPowerModel(0.0),
) : BareMetalDriver, FailureDomain, Powerable {
    /**
     * The flavor that corresponds to this machine.
     */
    private val flavor = Flavor(
        machine.cpus.size,
        machine.memory.map { it.size }.sum()
    )

    /**
     * The events of the machine.
     */
    private val events = EventFlow<NodeEvent>()

    /**
     * The machine state.
     */
    private val nodeState =
        StateFlow(Node(uid, name, metadata + ("driver" to this), NodeState.SHUTOFF, flavor, Image.EMPTY, events))

    /**
     * The [SimBareMetalMachine] we use to run the workload.
     */
    private val machine = SimBareMetalMachine(coroutineScope, clock, machine)

    override val node: Flow<Node> = nodeState

    override val usage: Flow<Double>
        get() = this.machine.usage

    override val powerDraw: Flow<Double> = cpuPowerModel.getPowerDraw(this)

    /**
     * The [Job] that runs the simulated workload.
     */
    private var job: Job? = null

    override suspend fun init(): Node {
        return nodeState.value
    }

    override suspend fun start(): Node {
        val node = nodeState.value
        if (node.state != NodeState.SHUTOFF) {
            return node
        }

        val workload = node.image.tags["workload"] as SimWorkload

        job = coroutineScope.launch {
            delay(1) // TODO Introduce boot time
            initMachine()
            try {
                machine.run(workload, mapOf("driver" to this@SimBareMetalDriver, "node" to node))
                exitMachine(null)
            } catch (_: CancellationException) {
                // Ignored
            } catch (cause: Throwable) {
                exitMachine(cause)
            }
        }

        setNode(node.copy(state = NodeState.BOOT))
        return nodeState.value
    }

    private fun initMachine() {
        setNode(nodeState.value.copy(state = NodeState.ACTIVE))
    }

    private fun exitMachine(cause: Throwable?) {
        val newNodeState =
            if (cause == null)
                NodeState.SHUTOFF
            else
                NodeState.ERROR
        setNode(nodeState.value.copy(state = newNodeState))
    }

    override suspend fun stop(): Node {
        val node = nodeState.value
        if (node.state == NodeState.SHUTOFF) {
            return node
        }

        job?.cancelAndJoin()
        setNode(node.copy(state = NodeState.SHUTOFF))
        return node
    }

    override suspend fun reboot(): Node {
        stop()
        return start()
    }

    override suspend fun setImage(image: Image): Node {
        setNode(nodeState.value.copy(image = image))
        return nodeState.value
    }

    override suspend fun refresh(): Node = nodeState.value

    private fun setNode(value: Node) {
        val field = nodeState.value
        if (field.state != value.state) {
            events.emit(NodeEvent.StateChanged(value, field.state))
        }

        nodeState.value = value
    }

    override val scope: CoroutineScope
        get() = coroutineScope

    override suspend fun fail() {
        setNode(nodeState.value.copy(state = NodeState.ERROR))
    }

    override suspend fun recover() {
        setNode(nodeState.value.copy(state = NodeState.ACTIVE))
    }

    override fun toString(): String = "SimBareMetalDriver(node = ${nodeState.value.uid})"
}

/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package nl.atlarge.opendc

import nl.atlarge.opendc.kernel.Kernel
import nl.atlarge.opendc.kernel.Simulator
import nl.atlarge.opendc.kernel.impl.MachineKernel
import nl.atlarge.opendc.topology.AdjacencyListTopologyBuilder
import nl.atlarge.opendc.topology.Component
import nl.atlarge.opendc.topology.container.Rack
import nl.atlarge.opendc.topology.machine.Cpu
import nl.atlarge.opendc.topology.machine.Machine
import org.junit.jupiter.api.Test

internal class SmokeTest {
    @Test
    fun smoke() {
		val mapping: MutableMap<Component<*>, Class<out Kernel<*>>> = HashMap()
		val builder = AdjacencyListTopologyBuilder()
		val topology = builder.build().apply {
			val rack = node(Rack())
			val machineA = node(Machine())
			val machineB = node(Machine())

			connect(rack, machineA, tag = "machine")
			connect(rack, machineB, tag = "machine")

			val cpuA1 = node(Cpu(10, 2, 2))
			val cpuA2 = node(Cpu(5, 3, 2))

			connect(machineA, cpuA1, tag = "cpu")
			connect(machineA, cpuA2, tag = "cpu")

			val cpuB1 = node(Cpu(10, 2, 2))
			val cpuB2 = node(Cpu(5, 3, 2))

			connect(machineB, cpuB1, tag = "cpu")
			connect(machineB, cpuB2, tag = "cpu")

			mapping.apply {
				put(machineA, MachineKernel::class.java)
				put(machineB, MachineKernel::class.java)
			}

			val simulator = Simulator(this, mapping)
			while (simulator.hasNext()) {
				simulator.next()
			}
		}
	}
}

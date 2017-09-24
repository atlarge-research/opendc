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

import nl.atlarge.opendc.kernel.omega.OmegaKernel
import nl.atlarge.opendc.scheduler.FifoScheduler
import nl.atlarge.opendc.scheduler.SrtfScheduler
import nl.atlarge.opendc.topology.AdjacencyList
import nl.atlarge.opendc.topology.TopologyListener
import nl.atlarge.opendc.topology.container.Datacenter
import nl.atlarge.opendc.topology.container.Rack
import nl.atlarge.opendc.topology.container.Room
import nl.atlarge.opendc.topology.machine.Cpu
import nl.atlarge.opendc.topology.machine.Machine
import nl.atlarge.opendc.workload.Task
import org.junit.jupiter.api.Test
import java.util.*

internal class SmokeTest {
	@Test
	fun smoke() {
		val datacenter = Datacenter(SrtfScheduler(), 50)
		val builder = AdjacencyList.builder()
		val topology = builder.construct {
			add(datacenter)

			// Add a room to the datacenter
			val room = Room().also {
				add(it)
				connect(datacenter, it, tag = "room")
			}

			// Add a rack to the room
			val rack = Rack().also {
				add(it)
				connect(room, it, tag = "rack")
			}

			val n = 10

			// Create n machines in the rack
			repeat(n) {
				val machine = Machine().also {
					add(it)
					connect(rack, it, tag = "machine")
				}

				val cpu1 = Cpu(10, 2, 2)
				val cpu2 = Cpu(5, 3, 2)
				add(cpu1)
				add(cpu2)

				connect(machine, cpu1, tag = "cpu")
				connect(machine, cpu2, tag = "cpu")
			}
		}

		val simulation = OmegaKernel.create(topology)
		val random = Random(0)
		for (i in 0..100) {
			val task = Task(i, emptySet(), random.nextInt(10000).toLong())
			simulation.schedule(task, datacenter, delay = random.nextInt(15000).toLong())
		}
		simulation.run(50000)

	}
}

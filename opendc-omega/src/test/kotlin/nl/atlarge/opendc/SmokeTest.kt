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

import nl.atlarge.opendc.kernel.Context
import nl.atlarge.opendc.kernel.Process
import nl.atlarge.opendc.kernel.omega.OmegaKernel
import nl.atlarge.opendc.topology.AdjacencyList
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.machine.Machine
import org.junit.jupiter.api.Test

/**
 * This test suite checks for smoke when running a large amount of simulations.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal class SmokeTest {
	/**
	 * Run a large amount of simulations and test if any exceptions occur.
	 */
	@Test
	fun smoke() {
		val n = 1000
		val builder = AdjacencyList.builder()
		repeat(n) {
			val root = Machine()
			val topology = builder.construct {
				add(root)

				val other = Machine()
				add(other)

				connect(root, other, tag = "neighbour")
				connect(other, root, tag = "neighbour")
			}

			val simulation = OmegaKernel.create(topology)

			for (i in 1..1000) {
				simulation.schedule(i, root, delay = i.toLong())
			}

			simulation.run()
		}
	}

	class NullProcess : Entity<Unit>, Process<NullProcess> {
		override val initialState = Unit
		suspend override fun Context<NullProcess>.run() {}
	}

	/**
	 * Test if the kernel allows sending messages to [Process] instances that have already stopped.
	 */
	@Test
	fun `sending message to process that has gracefully stopped`() {

		val builder = AdjacencyList.builder()
		val process = NullProcess()
		val topology = builder.construct {
			add(process)
		}

		val simulation = OmegaKernel.create(topology)
		simulation.schedule(0, process)
		simulation.run()
	}

	class CrashProcess : Entity<Unit>, Process<NullProcess> {
		override val initialState = Unit
		suspend override fun Context<NullProcess>.run() {
			TODO()
		}
	}

	/**
	 * Test if the kernel allows sending messages to [Process] instances that have crashed.
	 */
	@Test
	fun `sending message to process that has crashed`() {

		val builder = AdjacencyList.builder()
		val process = CrashProcess()
		val topology = builder.construct {
			add(process)
		}

		val simulation = OmegaKernel.create(topology)
		simulation.schedule(0, process)
		simulation.run()
	}
}

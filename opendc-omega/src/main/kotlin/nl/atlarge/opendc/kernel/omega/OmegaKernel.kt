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

package nl.atlarge.opendc.kernel.omega

import nl.atlarge.opendc.kernel.Kernel
import nl.atlarge.opendc.kernel.Process
import nl.atlarge.opendc.kernel.Simulation
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.MutableTopology
import nl.atlarge.opendc.topology.Topology

/**
 * The Omega simulation kernel is the reference simulation kernel implementation for the OpenDC Simulator core.
 *
 * This simulator implementation is a single-threaded implementation, running simulation kernels synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur in the entities.
 *
 * By default, [Process]s are resolved as part of the [Topology], meaning each [Entity] in the topology should also
 * implement its simulation behaviour by deriving from the [Process] interface.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
object OmegaKernel : Kernel {
	/**
	 * The name of the kernel.
	 */
	override val name: String = "opendc-omega"

	/**
	 * Create a new [Simulation] of the given [Topology] that is facilitated by this simulation kernel.
	 *
	 * @param topology The [Topology] to create a [Simulation] of.
	 * @return A [Simulation] instance.
	 */
	override fun create(topology: MutableTopology): Simulation = OmegaSimulation(this, topology)
}

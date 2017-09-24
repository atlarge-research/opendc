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

package nl.atlarge.opendc.kernel

import nl.atlarge.opendc.topology.MutableTopology
import nl.atlarge.opendc.topology.Topology

/**
 * A message-based discrete event simulator (DES). This interface allows running simulations over a [Topology].
 * This discrete event simulator works by having entities in a [Topology] interchange messages between each other and
 * updating their observable state accordingly.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Kernel {
	/**
	 * The name of the kernel.
	 */
	val name: String

	/**
	 * Create a new [Simulation] of the given [Topology] that is facilitated by this simulation kernel.
	 *
	 * @param topology The [Topology] to create a [Simulation] of.
	 * @return A [Simulation] instance.
	 */
	fun create(topology: MutableTopology): Simulation
}

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

package com.atlarge.opendc.omega

import com.atlarge.opendc.simulator.kernel.KernelFactory
import com.atlarge.opendc.simulator.kernel.Kernel
import com.atlarge.opendc.simulator.Bootstrap

/**
 * The Omega simulation kernel is the reference simulation kernel implementation for the OpenDC Simulator core.
 *
 * This simulator implementation is a single-threaded implementation, running simulation kernels synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur in the entities.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
object OmegaKernelFactory : KernelFactory {
	/**
	 * Create a simulation over the given model facilitated by this simulation kernel.
	 *
	 * @param bootstrap The bootstrap procedure to bootstrap the simulation with.
	 * @return A [Kernel] instance to control the simulation.
	 */
	override fun <M> create(bootstrap: Bootstrap<M>): Kernel<M> = OmegaKernel(bootstrap)
}

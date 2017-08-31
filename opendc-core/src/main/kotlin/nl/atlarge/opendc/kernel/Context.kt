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

import nl.atlarge.opendc.kernel.messaging.Readable
import nl.atlarge.opendc.topology.Component
import nl.atlarge.opendc.topology.Entity

/**
 * The [Context] interface provides a context for a simulation kernel, which defines the environment in which the
 * simulation is run.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Context<out T: Component<*>>: Readable {
	/**
	 * The [Component] that is simulated.
	 */
	val component: T

	/**
	 * The observable state of an [Entity] within the simulation is provided by context.
	 */
	val <S> Entity<S>.state: S

	/**
	 * Suspend the simulation kernel until the next tick occurs in the simulation.
	 */
	suspend fun tick(): Boolean {
		sleep(1)
		return true
	}

	/**
	 * Suspend the simulation kernel for <code>n</code> ticks before resuming the execution.
	 *
	 * @param n The amount of ticks to suspend the simulation kernel.
	 */
	suspend fun sleep(n: Int)
}

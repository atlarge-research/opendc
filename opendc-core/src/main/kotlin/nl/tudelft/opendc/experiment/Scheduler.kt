/*
 * MIT License
 *
 * Copyright (c) 2017 tudelft-atlarge
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

package nl.tudelft.opendc.experiment

import nl.tudelft.opendc.topology.Entity
import nl.tudelft.opendc.topology.Node

/**
 * A task scheduler that is coupled to an [Entity] in the topology of the cloud network.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Scheduler<in E: Entity> {
	/**
	 * Schedule the given jobs for the given entity.
	 *
	 * @param node The node in the cloud network topology representing the entity.
	 * @param jobs The jobs that have been submitted to the cloud network.
	 */
	fun schedule(node: Node<E>, jobs: Set<Job>)
}

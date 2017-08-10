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

package nl.atlarge.opendc.experiment.messaging

import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.Label

/**
 * A direct bi-directional communication channel between two [Entity] instances as seen from one of the entities.
 *
 * <p>A [Channel] is viewed as an edge that connects two entities in the topology of a cloud network.
 *
 * @param <E> The type of [Entity] this channel points to.
 * @param <T> The type of the label data of this channel.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Channel<out E: Entity, out T>: Pushable, Pullable {
	/**
	 * The [Entity] instance this channel is points to.
	 */
	val entity: E

	/**
	 * The label of the channel, possibly containing user-defined information.
	 */
	val label: Label<T>

	/**
	 * The channel the message originates from.
	 */
	val Receivable<Any?>.channel: Channel<E, T>
		get() = this@Channel
}

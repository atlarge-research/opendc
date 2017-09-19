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

import nl.atlarge.opendc.kernel.messaging.Envelope
import nl.atlarge.opendc.kernel.messaging.Receipt
import nl.atlarge.opendc.kernel.time.Instant
import nl.atlarge.opendc.topology.Entity

/**
 * A wrapper around a message that has been scheduled for processing.
 *
 * @property message The message to wrap.
 * @property time The point in time to deliver the message.
 * @property sender The sender of the message.
 * @property destination The destination of the message.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal data class MessageContainer(override val message: Any,
									 val time: Instant,
									 override val sender: Entity<*>?,
									 override val destination: Entity<*>) : Envelope<Any>, Receipt {
	/**
	 * A flag to indicate the message has been canceled.
	 */
	override var canceled: Boolean = false

	/**
	 * A flag to indicate the message has been delivered.
	 */
	override var delivered: Boolean = false

	/**
	 * Cancel the message to prevent it from being received by an [Entity].
	 *
	 * @throws IllegalStateException if the message has already been delivered.
	 */
	override fun cancel() {
		if (delivered) {
			throw IllegalStateException("The message has already been delivered")
		}

		canceled = true
	}

}

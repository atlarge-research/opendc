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

package nl.atlarge.opendc.kernel.messaging

import nl.atlarge.opendc.kernel.time.Duration


/**
 * A [Readable] instance has a mailbox associated with the instance to which objects can send messages, which can be
 * received by the class.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Readable {
	/**
	 * Retrieve and removes a single message from the entity's mailbox, suspending the function if the mailbox is empty.
	 * The execution is resumed after the message has landed in the entity's mailbox after which the message [Envelope]
	 * is mapped through `block` to generate a processed message.
	 *
	 * @param block The block to process the message with.
	 * @return The processed message.
	 */
	suspend fun <T> receive(block: Envelope<*>.(Any) -> T): T

	/**
	 * Retrieve and removes a single message from the entity's mailbox, suspending the function if the mailbox is empty.
	 * The execution is resumed after the message has landed in the entity's mailbox or the timeout was reached,
	 *
	 * If the message has been received, the message [Envelope] is mapped through `block` to generate a processed
	 * message. If the timeout was reached, `block` is not called and `null` is returned.
	 *
	 * @param timeout The duration to wait before resuming execution.
	 * @param block The block to process the message with.
	 * @return The processed message or `null` if the timeout was reached.
	 */
	suspend fun <T> receive(timeout: Duration, block: Envelope<*>.(Any) -> T): T?

	/**
	 * Retrieve and removes a single message from the entity's mailbox, suspending the function until a message has
	 * landed in the entity's mailbox.
	 *
	 * @return The message that was received from the entity's mailbox.
	 */
	suspend fun receive(): Any = receive { it }

	/**
	 * Retrieve and removes a single message from the entity's mailbox, suspending the function until a message has
	 * landed in the entity's mailbox or the timeout has been reached.
	 *
	 * @return The message that was received from the entity's mailbox or `null` if the timeout was reached.
	 */
	suspend fun receive(timeout: Duration): Any? = receive(timeout) { it }
}

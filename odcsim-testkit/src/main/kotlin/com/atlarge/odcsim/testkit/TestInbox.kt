/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.odcsim.testkit

import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Envelope

/**
 * A helper class for testing messages sent to an [ActorRef].
 *
 * @param T The shape of the messages the inbox accepts.
 */
interface TestInbox<T : Any> {
    /**
     * The actor reference of this inbox.
     */
    val ref: ActorRef<T>

    /**
     * A flag to indicate whether the inbox contains any messages.
     */
    val hasMessages: Boolean

    /**
     * Receive the oldest message from the inbox and remove it.
     *
     * @return The message that has been received.
     * @throws NoSuchElementException if the inbox is empty.
     */
    fun receiveMessage(): T = receiveEnvelope().message

    /**
     * Receive the oldest message from the inbox and remove it.
     *
     * @return The envelope containing the message that has been received.
     * @throws NoSuchElementException if the inbox is empty.
     */
    fun receiveEnvelope(): Envelope<T>

    /**
     * Receive all messages from the inbox and empty it.
     *
     * @return The list of messages in the inbox.
     */
    fun receiveAll(): List<Envelope<T>>

    /**
     * Clear all messages from the inbox.
     */
    fun clear()

    /**
     * Assert that the oldest message is equal to the [expected] message and remove
     * it from the inbox.
     *
     * @param expected The expected message to be the oldest in the inbox.
     */
    fun expectMessage(expected: T)

    /**
     * Assert that the oldest message is equal to the [expected] message and remove
     * it from the inbox.
     *
     * @param expected The expected message to be the oldest in the inbox.
     * @param message The failure message to fail with.
     */
    fun expectMessage(expected: T, message: String)
}

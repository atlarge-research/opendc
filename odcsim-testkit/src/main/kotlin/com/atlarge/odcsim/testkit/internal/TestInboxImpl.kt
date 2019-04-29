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

package com.atlarge.odcsim.testkit.internal

import com.atlarge.odcsim.ActorPath
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Envelope
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.testkit.TestInbox
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.PriorityQueue

/**
 * A helper class for testing messages sent to an [ActorRef].
 *
 * @param owner The owner of the inbox.
 * @param path The path to the test inbox.
 * @param T The shape of the messages the inbox accepts.
 */
internal class TestInboxImpl<T : Any>(private val owner: BehaviorTestKitImpl<*>, path: ActorPath) : TestInbox<T> {
    /**
     * The queue of received messages.
     */
    private val inbox = PriorityQueue<Envelope<T>>()

    /**
     * The identifier for the next message to be scheduled.
     */
    private var nextId: Long = 0

    override val ref: ActorRef<T> = ActorRefImpl(path)

    override val hasMessages: Boolean
        get() = inbox.isNotEmpty()

    override fun receiveEnvelope(): Envelope<T> = inbox.remove()

    override fun receiveAll(): List<Envelope<T>> = inbox.toList().also { inbox.clear() }

    override fun clear() = inbox.clear()

    override fun expectMessage(expected: T) = assertEquals(expected, receiveMessage())

    override fun expectMessage(expected: T, message: String) = assertEquals(expected, receiveMessage(), message)

    internal inner class ActorRefImpl(override val path: ActorPath) : ActorRef<T> {
        /**
         * Send the specified message to the actor this reference is pointing to after the specified delay.
         *
         * @param msg The message to send.
         * @param after The delay before the message is received.
         */
        fun send(msg: T, after: Instant) {
            inbox.add(EnvelopeImpl(nextId++, owner.time + after, msg))
        }
    }

    /**
     * A wrapper around a message that has been scheduled for processing.
     *
     * @property id The identifier of the message to keep the priority queue stable.
     * @property time The point in time to deliver the message.
     * @property message The message to wrap.
     */
    private inner class EnvelopeImpl(val id: Long,
                                     override val time: Instant,
                                     override val message: T) : Envelope<T> {
        override fun compareTo(other: Envelope<*>): Int {
            val cmp = super.compareTo(other)
            return if (cmp == 0 && other is EnvelopeImpl)
                id.compareTo(other.id)
            else
                cmp
        }
    }

}

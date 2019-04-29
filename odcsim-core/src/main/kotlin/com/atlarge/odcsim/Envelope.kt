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

package com.atlarge.odcsim

import java.io.Serializable

/**
 * A timestamped wrapper for messages that will be delivered to an actor.
 */
interface Envelope<T : Any> : Comparable<Envelope<*>>, Serializable {
    /**
     * The time at which this message should be delivered.
     */
    val time: Instant

    /**
     * The message contained in this envelope, of type [T]
     */
    val message: T

    /**
     * Extract the delivery time from the envelope.
     */
    operator fun component1(): Instant = time

    /**
     * Extract the message from this envelope.
     */
    operator fun component2(): T = message

    /**
     * Compare this envelope to the [other] envelope, ordered increasingly in time.
     */
    override fun compareTo(other: Envelope<*>): Int = time.compareTo(other.time)
}

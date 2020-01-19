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

/**
 * A communication endpoint of a specific logical process through which messages pass.
 *
 * Ports are tied to a specific logical process and may not be shared with other processes. Doing so results in
 * undefined behavior and might cause violation of time-consistency between processes.
 */
public interface Port {
    /**
     * Close this communication endpoint. This informs the process(es) at the other end of the port that the caller
     * will not send or receive messages via this port.
     *
     * This is an idempotent operation – subsequent invocations of this function have no effect and return `false`.
     */
    fun close(): Boolean
}

/**
 * A [Port] through which a logical process may receive messages from other [SendPort]s.
 */
public interface ReceivePort<out T : Any> : Port {
    /**
     * Receive a message send to this port or suspend the caller while no messages have been received at this port yet.
     */
    public suspend fun receive(): T
}

/**
 * A [Port] through which logical processes may send messages to a [ReceivePort].
 */
public interface SendPort<in T : Any> : Port {
    /**
     * Send a message via this port to the process(es) listening at the other end of the port.
     *
     * Messages are send asynchronously to the receivers and do not suspend the caller. This method guarantees
     * exactly-once delivery while respecting time-consistency between owner of the send port and its receivers.
     */
    public fun send(message: T)
}

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
 * A unidirectional communication medium using message passing. Processes may send messages over a channel, and
 * another process is able to receive messages sent over a channel it has a reference to (in form of a [ReceivePort]).
 *
 * Channels are represented by their send and receive endpoints at which processes may respectively send and receive
 * messages from this channel.
 *
 * Channels and their respective send and receive references may be shared freely between logical processes in the same
 * simulation.
 */
public interface Channel<T : Any> : Serializable {
    /**
     * The endpoint of the channel processes may use to send messages to.
     */
    public val send: SendRef<T>

    /**
     * The endpoint of the channel processes may receive messages from.
     */
    public val receive: ReceiveRef<T>

    /**
     * Obtain the send endpoint of the channel when unpacking the channel. See [send].
     */
    public operator fun component1(): SendRef<T> = send

    /**
     * Obtain the receive endpoint of the channel when unpacking the channel. See [receive].
     */
    public operator fun component2(): ReceiveRef<T> = receive
}

/**
 * An opaque object representing a [Channel] endpoint through which logical processes can send messages over the
 * channel.
 */
public interface SendRef<in T : Any> : Serializable

/**
 * An opaque object representing the receive endpoint of a [Channel].
 */
public interface ReceiveRef<out T : Any> : Serializable

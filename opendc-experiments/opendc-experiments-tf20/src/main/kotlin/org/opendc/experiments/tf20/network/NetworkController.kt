/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.experiments.tf20.network

import kotlinx.coroutines.channels.Channel
import org.opendc.common.Dispatcher
import org.opendc.common.util.TimerScheduler

/**
 * The network controller represents a simple network model between the worker and master nodes during
 * TensorFlow execution.
 */
public class NetworkController(dispatcher: Dispatcher) : AutoCloseable {
    /**
     * The scheduler for the message.
     */
    private val scheduler = TimerScheduler<Message>(dispatcher)

    /**
     * The outbound communication channels.
     */
    private val channels = mutableMapOf<NetworkNode, Channel<Message>>()

    /**
     * A map of the bandwidth between the different nodes.
     */
    private val bandwidthMatrix: MutableMap<Pair<NetworkNode, NetworkNode>, Long> = mutableMapOf()

    /**
     * A counter representing the amount of messages sent via the controller.
     */
    private var messageCounter = 0

    /**
     * Add the specified link to this controller.
     */
    public fun addLink(node: NetworkNode): Channel<Message> {
        val channel = Channel<Message>(Channel.UNLIMITED)
        channels[node] = channel
        return channel
    }

    /**
     * Add a connection between two links.
     */
    public fun addConnection(node1: NetworkNode, node2: NetworkNode, bandwidth: Long) {
        bandwidthMatrix[Pair(node1, node2)] = bandwidth
    }

    /**
     * Route the specified [message].
     */
    public fun send(message: Message) {
        val from = message.from
        val to = message.to
        val bandwidth = bandwidthMatrix[Pair(from, to)] ?: bandwidthMatrix[Pair(to, from)] ?: 1
        val size = message.dataSize / 1_000_000
        val delayTime = size / bandwidth + (0..5).random()

        messageCounter++

        val target = channels[to] ?: return // Drop if destination not found

        scheduler.startSingleTimer(message, delayTime) { target.trySend(message) }
    }

    /**
     * Stop the network controller.
     */
    override fun close() {
        scheduler.cancelAll()
    }
}

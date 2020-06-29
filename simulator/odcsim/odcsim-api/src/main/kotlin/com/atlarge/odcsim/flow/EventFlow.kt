/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.odcsim.flow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.consumeAsFlow
import java.util.WeakHashMap

/**
 * A [Flow] that can be used to emit events.
 */
public interface EventFlow<T> : Flow<T> {
    /**
     * Emit the specified [event].
     */
    public fun emit(event: T)

    /**
     * Close the flow.
     */
    public fun close()
}

/**
 * Creates a new [EventFlow].
 */
@Suppress("FunctionName")
public fun <T> EventFlow(): EventFlow<T> = EventFlowImpl()

/**
 * Internal implementation of the [EventFlow] class.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
private class EventFlowImpl<T> : EventFlow<T> {
    private var closed: Boolean = false
    private val subscribers = WeakHashMap<SendChannel<T>, Unit>()

    override fun emit(event: T) {
        synchronized(this) {
            for ((chan, _) in subscribers) {
                chan.offer(event)
            }
        }
    }

    override fun close() {
        synchronized(this) {
            closed = true

            for ((chan, _) in subscribers) {
                chan.close()
            }
        }
    }

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>) {
        val channel: Channel<T>
        synchronized(this) {
            if (closed) {
                return
            }

            channel = Channel(Channel.UNLIMITED)
            subscribers[channel] = Unit
        }
        channel.consumeAsFlow().collect(collector)
    }

    override fun toString(): String = "EventFlow"
}

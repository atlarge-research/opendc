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

package org.opendc.simulator.flow

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A consumer of a [FlowSource].
 */
public interface FlowConsumer {
    /**
     * A flag to indicate that the consumer is currently consuming a [FlowSource].
     */
    public val isActive: Boolean

    /**
     * The flow capacity of this consumer.
     */
    public val capacity: Double

    /**
     * The current flow rate of the consumer.
     */
    public val rate: Double

    /**
     * The current flow demand.
     */
    public val demand: Double

    /**
     * The flow counters to track the flow metrics of the consumer.
     */
    public val counters: FlowCounters

    /**
     * Start consuming the specified [source].
     *
     * @throws IllegalStateException if the consumer is already active.
     */
    public fun startConsumer(source: FlowSource)

    /**
     * Ask the consumer to pull its source.
     *
     * If the consumer is not active, this operation will be a no-op.
     */
    public fun pull()

    /**
     * Disconnect the consumer from its source.
     *
     * If the consumer is not active, this operation will be a no-op.
     */
    public fun cancel()
}

/**
 * Consume the specified [source] and suspend execution until the source is fully consumed or failed.
 */
public suspend fun FlowConsumer.consume(source: FlowSource) {
    return suspendCancellableCoroutine { cont ->
        startConsumer(object : FlowSource by source {
            override fun onEvent(conn: FlowConnection, now: Long, event: FlowEvent) {
                source.onEvent(conn, now, event)

                if (event == FlowEvent.Exit && !cont.isCompleted) {
                    cont.resume(Unit)
                }
            }

            override fun onFailure(conn: FlowConnection, cause: Throwable) {
                try {
                    source.onFailure(conn, cause)
                    cont.resumeWithException(cause)
                } catch (e: Throwable) {
                    e.addSuppressed(cause)
                    cont.resumeWithException(e)
                }
            }

            override fun toString(): String = "SuspendingFlowSource"
        })

        cont.invokeOnCancellation { cancel() }
    }
}

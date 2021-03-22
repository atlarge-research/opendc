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

package org.opendc.simulator.resources

import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A [SimResourceProvider] provides some resource of type [R].
 */
public interface SimResourceProvider : AutoCloseable {
    /**
     * The state of the resource.
     */
    public val state: SimResourceState

    /**
     * Start the specified [resource consumer][consumer] in the context of this resource provider asynchronously.
     *
     * @throws IllegalStateException if there is already a consumer active or the resource lifetime has ended.
     */
    public fun startConsumer(consumer: SimResourceConsumer)

    /**
     * Interrupt the resource consumer. If there is no consumer active, this operation will be a no-op.
     */
    public fun interrupt()

    /**
     * Cancel the current resource consumer. If there is no consumer active, this operation will be a no-op.
     */
    public fun cancel()

    /**
     * End the lifetime of the resource.
     *
     * This operation terminates the existing resource consumer.
     */
    public override fun close()
}

/**
 * Consume the resource provided by this provider using the specified [consumer] and suspend execution until
 * the consumer has finished.
 */
public suspend fun SimResourceProvider.consume(consumer: SimResourceConsumer) {
    return suspendCancellableCoroutine { cont ->
        startConsumer(object : SimResourceConsumer by consumer {
            override fun onFinish(ctx: SimResourceContext, cause: Throwable?) {
                assert(!cont.isCompleted) { "Coroutine already completed" }

                consumer.onFinish(ctx, cause)

                cont.resumeWith(if (cause != null) Result.failure(cause) else Result.success(Unit))
            }

            override fun toString(): String = "SimSuspendingResourceConsumer"
        })
    }
}

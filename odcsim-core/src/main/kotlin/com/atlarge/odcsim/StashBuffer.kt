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

import com.atlarge.odcsim.internal.StashBufferImpl

/**
 * A non thread safe mutable message buffer that can be used to buffer messages inside actors and then unstash them.
 *
 * @param T The shape of the messages in this buffer.
 */
interface StashBuffer<T : Any> {
    /**
     * The first element of the buffer.
     *
     * @throws NoSuchElementException if the buffer is empty.
     */
    val head: T

    /**
     * A flag to indicate whether the buffer is empty.
     */
    val isEmpty: Boolean

    /**
     * A flag to indicate whether the buffer is full.
     */
    val isFull: Boolean

    /**
     * The number of elements in the stash buffer.
     */
    val size: Int

    /**
     * Iterate over all elements of the buffer and apply a function to each element, without removing them.
     *
     * @param block The function to invoke for each element.
     */
    fun forEach(block: (T) -> Unit)

    /**
     * Add one element to the end of the message buffer.
     *
     * @param msg The message to stash.
     * @throws IllegalStateException if the element cannot be added at this time due to capacity restrictions
     */
    fun stash(msg: T)

    /**
     * Process all stashed messages with the behavior and the returned [Behavior] from each processed message.
     *
     * @param ctx The actor context to process these messages in.
     * @param behavior The behavior to process the messages with.
     */
    fun unstashAll(ctx: ActorContext<T>, behavior: Behavior<T>): Behavior<T>

    companion object {
        /**
         * Construct a [StashBuffer] with the specified [capacity].
         *
         * @param capacity The capacity of the buffer.
         */
        operator fun <T : Any> invoke(capacity: Int): StashBuffer<T> = StashBufferImpl(capacity)
    }
}

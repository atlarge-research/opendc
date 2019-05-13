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

package com.atlarge.odcsim.internal

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.StashBuffer
import java.util.ArrayDeque

/**
 * Internal implementation of the [StashBuffer] interface.
 */
internal class StashBufferImpl<T : Any>(private val capacity: Int) : StashBuffer<T> {
    /**
     * The internal queue used to store the messages.
     */
    private val queue = ArrayDeque<T>(capacity)

    override val head: T
        get() = queue.first

    override val isEmpty: Boolean
        get() = queue.isEmpty()

    override val isFull: Boolean
        get() = size > capacity

    override val size: Int
        get() = queue.size

    override fun forEach(block: (T) -> Unit) {
        queue.toList().forEach(block)
    }

    override fun stash(msg: T) {
        queue.add(msg)
    }

    override fun unstashAll(ctx: ActorContext<T>, behavior: Behavior<T>): Behavior<T> {
        val messages = queue.toList()
        queue.clear()

        val interpreter = BehaviorInterpreter<T>(behavior)
        interpreter.start(ctx)

        for (message in messages) {
            interpreter.interpretMessage(ctx, message)
        }

        return interpreter.behavior
    }
}

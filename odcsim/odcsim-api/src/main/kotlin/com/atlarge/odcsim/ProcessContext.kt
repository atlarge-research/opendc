/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
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

import java.time.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import org.slf4j.Logger

/**
 * Represents the execution context of a logical process in simulation.
 */
public interface ProcessContext : CoroutineContext.Element {
    /**
     * Key for [ProcessContext] instance in the coroutine context.
     */
    companion object Key : CoroutineContext.Key<ProcessContext>

    /**
     * The reference to the logical process of this context.
     */
    public val self: ProcessRef

    /**
     * The clock tracking the simulation time.
     */
    public val clock: Clock

    /**
     * A logger instance tied to the logical process.
     */
    public val log: Logger

    /**
     * Spawn an anonymous logical process in the simulation universe with the specified [behavior].
     */
    public fun spawn(behavior: Behavior): ProcessRef

    /**
     * Spawn a logical process in the simulation universe with the specified [behavior] and [name].
     */
    public fun spawn(behavior: Behavior, name: String): ProcessRef

    /**
     * Open a new communication [Channel] for messages of type [T].
     */
    public fun <T : Any> open(): Channel<T>

    /**
     * Create a [SendPort] for sending messages to the specified [send].
     */
    public suspend fun <T : Any> connect(send: SendRef<T>): SendPort<T>

    /**
     * Create a [ReceivePort] for listening to the messages sent to the specified [receive] endpoint of a channel.
     */
    public suspend fun <T : Any> listen(receive: ReceiveRef<T>): ReceivePort<T>
}

/**
 * The process context of the current coroutine.
 */
@Suppress("WRONG_MODIFIER_TARGET")
public suspend inline val processContext: ProcessContext
    @Suppress("ILLEGAL_SUSPEND_PROPERTY_ACCESS")
    get() = coroutineContext[ProcessContext.Key] ?: throw IllegalStateException("No process context active")

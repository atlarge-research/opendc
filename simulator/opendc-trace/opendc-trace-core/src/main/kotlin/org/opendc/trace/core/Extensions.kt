/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.trace.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Convert an [EventStream] to a [Flow] of [Event]s but do not start collection of the stream.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun EventStream.asFlow(): Flow<Event> = callbackFlow {
    onEvent { sendBlocking(it) }
    onError { cancel(CancellationException("API error", it)) }
    onClose { channel.close() }
    awaitClose { this@asFlow.close() }
}

/**
 * Convert an [EventStream] to a [Flow] of [Event]s but do not start collection of the stream.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun EventStream.consumeAsFlow(): Flow<Event> = callbackFlow {
    onEvent { sendBlocking(it) }
    onError { cancel(CancellationException("API error", it)) }
    start()
}

/**
 * Convert an [EventStream] to a [Flow] of [Event] of type [E] but do not start collection of the stream.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public inline fun <reified E : Event> EventStream.asTypedFlow(): Flow<E> = callbackFlow {
    onEvent<E> { sendBlocking(it) }
    onError { cancel(CancellationException("API error", it)) }
    onClose { channel.close() }
    awaitClose { this@asTypedFlow.close() }
}

/**
 * Convert an [EventStream] to a [Flow] of [Event] of type [E] but do not start collection of the stream.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public inline fun <reified E : Event> EventStream.consumeAsTypedFlow(): Flow<E> = callbackFlow {
    onEvent<E> { sendBlocking(it) }
    onError { cancel(CancellationException("API error", it)) }
    start()
}

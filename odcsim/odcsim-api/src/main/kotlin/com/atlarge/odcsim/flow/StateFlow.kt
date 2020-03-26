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
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow

/**
 * A [Flow] that contains a single value that changes over time.
 *
 * This class exists to implement the DataFlow/StateFlow functionality that will be implemented in `kotlinx-coroutines`
 * in the future, but is not available yet.
 * See: https://github.com/Kotlin/kotlinx.coroutines/pull/1354
 */
public interface StateFlow<T> : Flow<T> {
    /**
     * The current value of this flow.
     *
     * Setting a value that is [equal][Any.equals] to the previous one does nothing.
     */
    public var value: T
}

/**
 * Creates a [StateFlow] with a given initial [value].
 */
@Suppress("FunctionName")
public fun <T> StateFlow(value: T): StateFlow<T> = StateFlowImpl(value)

/**
 * Internal implementation of the [StateFlow] interface.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
private class StateFlowImpl<T>(initialValue: T) : StateFlow<T> {
    /**
     * The [BroadcastChannel] to back this flow.
     */
    private val chan = ConflatedBroadcastChannel(initialValue)

    /**
     * The internal [Flow] backing this flow.
     */
    private val flow = chan.asFlow()

    public override var value: T = initialValue
        set(value) {
            chan.offer(value)
            field = value
        }

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>) = flow.collect(collector)
}

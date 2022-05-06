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

package org.opendc.experiments.tf20.core

import java.util.*

/**
 * A compute device on which tensor operations are performed.
 */
public interface TFDevice : AutoCloseable {
    /**
     * The unique identifier of the device.
     */
    public val uid: UUID

    /**
     * A flag to indicate whether the device is a GPU.
     */
    public val isGpu: Boolean

    /**
     * Transfer the specified amount of data from memory.
     */
    public suspend fun load(dataSize: Long)

    /**
     * Perform [flops] amount of computation on the device.
     */
    public suspend fun compute(flops: Double)

    /**
     * Collect device statistics.
     */
    public fun getDeviceStats(): TFDeviceStats
}

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

package org.opendc.experiments.tf20.keras.shape

import kotlin.math.abs

/**
 * Represents the shape of a tensor.
 *
 * @param dims The sizes of the tensor dimensions.
 */
public class TensorShape(vararg dims: Long) {
    /**
     * The dimensions of the tensor represented as [LongArray].
     */
    private val _dims: LongArray = dims

    /**
     * Return amount of elements in Tensor with the given shape.
     */
    public val numElements: Long
        get() {
            var prod = 1L
            for (i in 0 until rank) {
                prod *= abs(_dims[i])
            }
            return prod
        }

    /**
     * Returns the rank of this shape.
     */
    public val rank: Int
        get() = _dims.size

    /**
     * Returns the value of a dimension
     *
     * @param i The index at which to retrieve a dimension.
     * @return The size of dimension i
     */
    public operator fun get(i: Int): Long {
        return _dims[i]
    }

    /**
     * Test whether dimension i in this shape is known
     *
     * @param i Target dimension to test
     * @return Whether dimension i is unknown (equal to -1)
     */
    private fun isKnown(i: Int): Boolean {
        return _dims[i] != -1L
    }

    /**
     * Get the size of a target dimension.
     *
     * @param i Target dimension.
     * @return The size of dimension i
     */
    public fun size(i: Int): Long {
        return _dims[i]
    }

    /**
     * Clone the [TensorShape] and return a new instance.
     */
    public fun clone(): TensorShape {
        return TensorShape(*_dims)
    }

    /**
     * Create a string representation of this [TensorShape].
     */
    override fun toString(): String {
        return _dims.contentToString().replace("-1", "None")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TensorShape

        if (!_dims.contentEquals(other._dims)) return false

        return true
    }

    override fun hashCode(): Int {
        return _dims.contentHashCode()
    }
}

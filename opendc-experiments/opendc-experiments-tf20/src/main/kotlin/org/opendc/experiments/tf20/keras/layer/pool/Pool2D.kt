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

package org.opendc.experiments.tf20.keras.layer.pool

import org.opendc.experiments.tf20.keras.layer.Layer
import org.opendc.experiments.tf20.keras.layer.conv.ConvPadding
import org.opendc.experiments.tf20.keras.shape.TensorShape
import kotlin.math.ceil

/**
 * Max pooling layer for 2D inputs (e.g. images).
 *
 * @property [poolSize] The size of the sliding window for each dimension of input tensor (pool batch, pool height, pool width, pool channels).
 * Usually, pool batch and pool channels are equal to 1.
 * @property [strides] Strides of the pooling operation for each dimension of input tensor.
 * @property [padding] The padding method, either 'valid' or 'same' or 'full'.
 * @property [name] Custom layer name.
 */
public class Pool2D(
    public val poolSize: IntArray = intArrayOf(1, 2, 2, 1),
    public val strides: IntArray = intArrayOf(1, 2, 2, 1),
    public val padding: ConvPadding = ConvPadding.VALID,
    name: String
) : Layer(name) {

    private var padHeight = 0L
    private var padWidth = 0L

    override fun build(inputShape: TensorShape) {
    }

    override fun getOutputShape(inputShape: TensorShape): TensorShape {
        var outHeight = 0L
        var outWidth = 0L
        // return the output tensor shape
        if (padding == ConvPadding.VALID) {
            outHeight = ceil((inputShape[1] - poolSize[1] + 1).toDouble() / strides[1].toDouble()).toLong()
            outWidth = ceil((inputShape[2] - poolSize[2] + 1).toDouble() / strides[2].toDouble()).toLong()
            padHeight = 0
            padWidth = 0
        } else if (padding == ConvPadding.SAME) {
            outHeight = ceil(inputShape[1].toFloat() / strides[1].toFloat()).toLong()
            outWidth = ceil(inputShape[2].toFloat() / strides[2].toFloat()).toLong()
            val padAlongHeight = (outHeight - 1) * strides[1] + poolSize[1] - inputShape[1]
            val padAlongWidth = (outWidth - 1) * strides[2] + poolSize[2] - inputShape[2]

            padHeight = padAlongHeight / 2
            padWidth = padAlongWidth / 2
        }

        return TensorShape(inputShape[0], outHeight, outWidth, inputShape[3])
    }

    override fun forward(): Double {
        val output = outputTensor
        // Per output pixel: kernel_w x kernel_h x in_channel
        var flops: Long = 2 * poolSize[1] * poolSize[2] * inputTensor[3]

        // Flops per output map.
        flops *= output[2] * output[1]

        // Flops across multiple input patches.
        flops *= inputTensor[0]

        return flops * 4.0 / 1_000_000
    }

    override fun backward(): Double = forward()

    override fun toString(): String {
        return "MaxPool2D[poolSize=${poolSize.contentToString()}, strides=${strides.contentToString()}, padding=$padding]"
    }
}

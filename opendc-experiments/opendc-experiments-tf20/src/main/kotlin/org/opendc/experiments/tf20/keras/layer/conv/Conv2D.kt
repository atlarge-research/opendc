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

package org.opendc.experiments.tf20.keras.layer.conv

import org.opendc.experiments.tf20.keras.activations.Activation
import org.opendc.experiments.tf20.keras.layer.Layer
import org.opendc.experiments.tf20.keras.shape.TensorShape
import kotlin.math.ceil

/**
 * 2D convolution layer (e.g. spatial convolution over images).
 *
 * This layer creates a convolution kernel that is convolved (actually cross-correlated)
 * with the layer input to produce a tensor of outputs.
 * Finally, if `activation` is applied to the outputs as well.
 */
public class Conv2D(
    public val filter: LongArray = LongArray(4), // [H, W, channel_in, channel_out]
    public val strides: LongArray = LongArray(4), // [1, stride_h, stride_w, 1]
    public val activation: Activation = Activation.Relu,
    public val padding: ConvPadding = ConvPadding.VALID,
    name: String = ""
) : Layer(name) {

    private var padHeight: Double = 0.0
    private var padWidth: Double = 0.0

    override fun build(inputShape: TensorShape) {}

    override fun getOutputShape(inputShape: TensorShape): TensorShape {
        check(filter[2] == inputShape[3]) { "Input channel ${filter[2]} and ${inputShape[3]} shall match" }

        var outHeight = 0L
        var outWidth = 0L

        if (padding == ConvPadding.VALID) {
            outHeight = ceil((inputShape[1] - filter[0] + 1).toDouble() / strides[1].toDouble()).toLong()
            outWidth = ceil((inputShape[2] - filter[1] + 1).toDouble() / strides[2].toDouble()).toLong()
            padHeight = 0.0
            padWidth = 0.0
        } else if (padding == ConvPadding.SAME) {
            outHeight = ceil(inputShape[1].toFloat() / strides[1].toFloat()).toLong()
            outWidth = ceil(inputShape[2].toFloat() / strides[2].toFloat()).toLong()

            val padAlongHeight = (outHeight - 1) * strides[1] + filter[0] - inputShape[1]
            val padAlongWidth = (outWidth - 1) * strides[2] + filter[1] - inputShape[2]

            padHeight = (padAlongHeight / 2).toDouble()
            padWidth = (padAlongWidth / 2).toDouble()
        }

        return TensorShape(inputShape[0], outHeight, outWidth, filter[3])
    }

    override fun forward(): Double {
        // Mul and add per output pixel: kernel_w x kernel_h x in_channel
        var flops: Long = (2 * filter[0] * filter[1] * filter[2])

        val output = outputTensor
        // Flops per output map.
        flops *= output[1] * output[2] * filter[3]

        // Flops across multiple input patches.
        flops *= inputTensor[0]

        if (activation == Activation.Relu) {
            flops += output[0] * output[1] * output[2] * output[3]
        }

        // return paramsNum() * output.H * output.W * FLOAT_BYTES / MILLION
        return flops * 4.0 / 1_000_000
    }

    override fun backward(): Double = forward()

    override fun toString(): String {
        return "Conv2D[filter=${filter.contentToString()}, strides=${strides.contentToString()}, activation=$activation, padding=$padding]"
    }
}

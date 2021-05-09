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

package org.opendc.experiments.tf20.keras.layer.regularization

import org.opendc.experiments.tf20.keras.layer.Layer
import org.opendc.experiments.tf20.keras.shape.TensorShape

/**
 * This layer applies dropout to the input.
 *
 * Dropout consists in randomly setting a fraction `rate` of input units to 0
 * at each update during training time, which helps prevent overfitting.
 * The units that are kept are scaled by `1 / (1 - rate)`, so that their
 * sum is unchanged at training time and inference time.
 *
 * @property keepProbability The dropout rate, between 0 and 1. E.g. `rate=0.1` would drop out 10% of input units.
 * @property [name] Custom layer name.
 */
public class Dropout(
    public val keepProbability: Float = 0.1f,
    name: String
) : Layer(name) {
    override fun build(inputShape: TensorShape) {}

    override fun getOutputShape(inputShape: TensorShape): TensorShape {
        return inputShape
    }

    override fun forward(): Double {
        val output = outputTensor
        return output[0] * output[1] * output[2] * output[3] * 4.0 / 1_000_000
    }

    override fun backward(): Double = forward()

    override fun toString(): String = "Dropout[keepProbability=$keepProbability]"
}

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

package org.opendc.experiments.tf20.keras.layer.core

import org.opendc.experiments.tf20.keras.activations.Activation
import org.opendc.experiments.tf20.keras.layer.Layer
import org.opendc.experiments.tf20.keras.shape.TensorShape

/**
 * This layer applies an activation function to an output.
 */
public class ActivationLayer(
    public val activation: Activation = Activation.Relu,
    name: String = ""
) : Layer(name) {

    override fun build(inputShape: TensorShape) {
        // Intentionally left empty
    }

    override fun getOutputShape(inputShape: TensorShape): TensorShape = inputShape

    override fun forward(): Double = 0.0

    override fun backward(): Double = forward()

    override fun toString(): String {
        return "ActivationLayer[activation=$activation]"
    }
}

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

package org.opendc.experiments.tf20.keras

import org.opendc.experiments.tf20.keras.layer.Layer
import org.opendc.experiments.tf20.keras.layer.core.Input
import org.opendc.experiments.tf20.keras.shape.TensorShape

/**
 * Sequential model groups a linear stack of layers into a TensorFlow TrainableModel.
 *
 * @param [layers] The layers to describe the model design.
 */
public class Sequential(vararg layers: Layer) : TrainableModel(*layers) {
    override fun buildLayers() {
        val inputShape = TensorShape(*inputLayer.packedDims)
        inputLayer.inputTensor = inputShape
        inputLayer.build(inputShape)
        var nextShape: TensorShape = inputLayer.getOutputShape(inputShape)
        inputLayer.outputTensor = nextShape

        layers.filter { it !is Input }.forEach {
            it.inputTensor = nextShape
            it.build(nextShape)

            nextShape = it.getOutputShape(nextShape)
            it.outputTensor = nextShape
        }
    }

    override fun forward(): Double {
        return layers.sumOf { it.forward() }
    }

    override fun backward(): Double {
        return layers.sumOf { it.backward() }
    }
}

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

import org.opendc.experiments.tf20.distribute.Strategy
import org.opendc.experiments.tf20.keras.layer.Layer
import org.opendc.experiments.tf20.keras.layer.core.Input

/**
 * A model groups layers into an object with training and inference features.
 */
public abstract class TrainableModel(vararg layers: Layer) : AutoCloseable {
    /**
     * The layers to describe the model design. Main part of the internal state of the model.
     */
    public val layers: List<Layer> = listOf(*layers)

    /**
     * First layer that is responsible for the input shape of the Neural Network.
     */
    public val inputLayer: Input
        get() = layers[0] as Input

    /**
     * Returns input dimensions in order HWC (height, width, channels)
     */
    public val inputDimensions: LongArray
        get() = (layers[0] as Input).packedDims

    /**
     * Layers indexed by name.
     */
    protected val layersByName: MutableMap<String, Layer> = mutableMapOf()

    /**
     * A flag to indicate that the model is compiled.
     */
    public var isCompiled: Boolean = false
        private set

    /**
     * The strategy that is being used.
     */
    private lateinit var strategy: Strategy

    /**
     * Common method for building the initial part of the model static graph.
     */
    protected abstract fun buildLayers()

    /**
     * Perform a forward propagation.
     */
    protected abstract fun forward(): Double

    /**
     * Perform a backward propagation.
     */
    protected abstract fun backward(): Double

    init {
        for (layer in layers) {
            if (layersByName.containsKey(layer.name)) {
                throw IllegalArgumentException(layer.name)
            } else {
                layersByName[layer.name] = layer
            }

            layer.parentModel = this
        }
    }

    /**
     * Configures the model for training.
     *
     * @param strategy The distribution strategy for training.
     */
    public fun compile(strategy: Strategy) {
        check(!isCompiled) { "Model is already compiled." }

        buildLayers()

        this.strategy = strategy
        this.isCompiled = true
    }

    /**
     * Train the model for a fixed number of [epochs] (iterations over a dataset).
     *
     * @param [epochs] Number of epochs to train the model. An epoch is an iteration over the entire x and y data provided.
     * @param [batchSize] Number of samples per gradient update.
     */
    public suspend fun fit(epochs: Int = 5, batchSize: Int = 32) {
        check(isCompiled) { "Model not yet compiled." }

        val forwardFlops = forward()
        val backwardFlops = backward()

        for (i in 1..epochs) {
            strategy.run(forwardFlops, backwardFlops, batchSize)
        }
    }

    override fun close() {
    }

    override fun toString(): String {
        return "TrainableModel ${super.toString()}"
    }
}

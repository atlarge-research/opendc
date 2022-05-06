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

package org.opendc.experiments.tf20

import org.opendc.experiments.tf20.keras.Sequential
import org.opendc.experiments.tf20.keras.TrainableModel
import org.opendc.experiments.tf20.keras.activations.Activation
import org.opendc.experiments.tf20.keras.layer.conv.Conv2D
import org.opendc.experiments.tf20.keras.layer.conv.ConvPadding
import org.opendc.experiments.tf20.keras.layer.core.ActivationLayer
import org.opendc.experiments.tf20.keras.layer.core.Input
import org.opendc.experiments.tf20.keras.layer.pool.Pool2D
import org.opendc.experiments.tf20.keras.layer.regularization.Dropout

/**
 * Construct an AlexNet model with the given batch size.
 */
fun AlexNet(batchSize: Long): TrainableModel {
    return Sequential(
        Input(batchSize, 227, 227, 3, name = "Input"),
        Conv2D(longArrayOf(11, 11, 3, 96), longArrayOf(1, 4, 4, 1), padding = ConvPadding.VALID, name = "conv1"),
        Pool2D(intArrayOf(1, 3, 3, 1), intArrayOf(1, 2, 2, 1), padding = ConvPadding.VALID, name = "pool1"),
        Conv2D(longArrayOf(5, 5, 96, 256), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv2"),
        Pool2D(intArrayOf(1, 3, 3, 1), intArrayOf(1, 2, 2, 1), padding = ConvPadding.VALID, name = "pool2"),
        Conv2D(longArrayOf(3, 3, 256, 384), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv3"),
        Conv2D(longArrayOf(3, 3, 384, 384), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv4"),
        Conv2D(longArrayOf(3, 3, 384, 256), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv5"),
        Pool2D(intArrayOf(1, 3, 3, 1), intArrayOf(1, 2, 2, 1), padding = ConvPadding.VALID, name = "pool5"),
        Conv2D(longArrayOf(6, 6, 256, 4096), longArrayOf(1, 1, 1, 1), padding = ConvPadding.VALID, name = "fc6"),
        Dropout(0.5f, name = "dropout6"),
        Conv2D(longArrayOf(1, 1, 4096, 4096), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "fc7"),
        Dropout(0.5f, name = "dropout7"),
        Conv2D(longArrayOf(1, 1, 4096, 1000), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "f8"),
        ActivationLayer(Activation.Softmax, name = "softmax")
    )
}

/**
 * Construct an VGG16 model with the given batch size.
 */
fun VGG16(batchSize: Long = 128): TrainableModel {
    return Sequential(
        Input(batchSize, 224, 224, 3, name = "Input"),
        Conv2D(longArrayOf(3, 3, 3, 64), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv1-1"),
        Conv2D(longArrayOf(3, 3, 64, 64), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv1-2"),
        Pool2D(intArrayOf(1, 2, 2, 1), intArrayOf(1, 2, 2, 1), padding = ConvPadding.VALID, name = "pool1"),
        Conv2D(longArrayOf(3, 3, 64, 128), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv2-1"),
        Conv2D(longArrayOf(3, 3, 128, 128), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv2-2"),
        Pool2D(intArrayOf(1, 2, 2, 1), intArrayOf(1, 2, 2, 1), padding = ConvPadding.VALID, name = "pool2"),
        Conv2D(longArrayOf(3, 3, 128, 256), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv3-1"),
        Conv2D(longArrayOf(3, 3, 256, 256), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv3-2"),
        Conv2D(longArrayOf(3, 3, 256, 256), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv3-3"),
        Pool2D(intArrayOf(1, 2, 2, 1), intArrayOf(1, 2, 2, 1), padding = ConvPadding.VALID, name = "pool3"),
        Conv2D(longArrayOf(3, 3, 256, 512), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv4-1"),
        Conv2D(longArrayOf(3, 3, 512, 512), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv4-2"),
        Conv2D(longArrayOf(3, 3, 512, 512), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv4-3"),
        Pool2D(intArrayOf(1, 2, 2, 1), intArrayOf(1, 2, 2, 1), padding = ConvPadding.VALID, name = "pool4"),
        Conv2D(longArrayOf(3, 3, 512, 512), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv5-1"),
        Conv2D(longArrayOf(3, 3, 512, 512), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv5-2"),
        Conv2D(longArrayOf(3, 3, 512, 512), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "conv5-3"),
        Pool2D(intArrayOf(1, 2, 2, 1), intArrayOf(1, 2, 2, 1), padding = ConvPadding.VALID, name = "pool5"),
        Conv2D(longArrayOf(7, 7, 512, 4096), longArrayOf(1, 1, 1, 1), padding = ConvPadding.VALID, name = "fc6"),
        Dropout(0.5f, name = "dropout6"),
        Conv2D(longArrayOf(1, 1, 4096, 4096), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "fc7"),
        Dropout(0.5f, name = "dropout7"),
        Conv2D(longArrayOf(1, 1, 4096, 1000), longArrayOf(1, 1, 1, 1), padding = ConvPadding.SAME, name = "f8"),
        ActivationLayer(Activation.Softmax, name = "softmax")
    )
}

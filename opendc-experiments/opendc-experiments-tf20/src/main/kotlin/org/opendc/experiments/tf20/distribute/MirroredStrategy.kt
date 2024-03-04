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

package org.opendc.experiments.tf20.distribute

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.opendc.experiments.tf20.core.TFDevice

/**
 * A distribution [Strategy] that supports synchronous distributed training on multiple GPUs on one machine.
 *
 * It creates one replica per GPU device. Each variable in the model is mirrored across all the replicas.
 */
public class MirroredStrategy(val devices: List<TFDevice>) : Strategy {
    override suspend fun run(
        forward: Double,
        backward: Double,
        batchSize: Int,
    ) = coroutineScope {
        for (device in devices) {
            launch { device.compute(forward * batchSize / devices.size + backward) }
        }
    }
}

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

package org.opendc.compute.simulator.scheduler.weights

import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask

/**
 * A [HostWeigher] that weighs the hosts based on the difference required vCPU capacity and the available CPU capacity.
 */
public class VGpuCapacityWeigher(override val multiplier: Double = 1.0) : HostWeigher {
    override fun getWeight(
        host: HostView,
        task: ServiceTask,
    ): Double {
        val model = host.host.getModel()
        val requiredCapacity = task.flavor.meta["gpu-capacity"] as? Double ?: 0.0
        val availableCapacity = model.gpuHostModels.maxOfOrNull { it.gpuCoreCapacity } ?: 0.0
        return availableCapacity - requiredCapacity / task.flavor.gpuCoreCount
    }

    override fun toString(): String = "VGpuWeigher"
}

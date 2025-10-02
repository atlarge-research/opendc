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

package org.opendc.compute.simulator.scheduler.filters

import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask

/**
 * A [HostFilter] that filters hosts based on the memory requirements of a [ServiceTask] and the RAM available on the host.
 *
 * @param allocationRatio Virtual RAM to physical RAM allocation ratio.
 */
public class RamFilter(private val allocationRatio: Double = 1.0) : HostFilter {
    private val isSimple = allocationRatio == 1.0

    override fun test(
        host: HostView,
        task: ServiceTask,
    ): Boolean {
        if (isSimple) return host.availableMemory >= task.flavor.memorySize

        val requestedMemory = task.flavor.memorySize
        val availableMemory = host.availableMemory
        val memoryCapacity = host.host.getModel().memoryCapacity

        // Do not allow an instance to overcommit against itself, only against
        // other instances.
        if (requestedMemory > memoryCapacity) {
            return false
        }

        val limit = memoryCapacity * allocationRatio
        val used = memoryCapacity - availableMemory
        val usable = limit - used

        val result = usable >= requestedMemory
        return result
    }

    override fun score(host: HostView): Double {
        return if (isSimple) {
            return host.availableMemory.toDouble()
        } else {
            host.host.getModel().memoryCapacity * allocationRatio
        }
    }
}

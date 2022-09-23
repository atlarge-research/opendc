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

package org.opendc.simulator.compute.model

/**
 * A description of the physical or virtual machine on which a bootable image runs.
 *
 * @property cpus The list of processing units available to the image.
 * @property memory The list of memory units available to the image.
 * @property net A list of network adapters available to the machine.
 * @property storage A list of storage devices available to the machine.
 */
public data class MachineModel(
    public val cpus: List<ProcessingUnit>,
    public val memory: List<MemoryUnit>,
    public val net: List<NetworkAdapter> = emptyList(),
    public val storage: List<StorageDevice> = emptyList()
) {
    /**
     * Optimize the [MachineModel] by merging all resources of the same type into a single resource with the combined
     * capacity. Such configurations can be simulated more efficiently by OpenDC.
     */
    public fun optimize(): MachineModel {
        val originalCpu = cpus[0]
        val freq = cpus.sumOf { it.frequency }
        val processingNode = originalCpu.node.copy(coreCount = 1)
        val processingUnits = listOf(originalCpu.copy(frequency = freq, node = processingNode))

        val memorySize = memory.sumOf { it.size }
        val memoryUnits = listOf(MemoryUnit("Generic", "Generic", 3200.0, memorySize))

        return MachineModel(processingUnits, memoryUnits)
    }
}

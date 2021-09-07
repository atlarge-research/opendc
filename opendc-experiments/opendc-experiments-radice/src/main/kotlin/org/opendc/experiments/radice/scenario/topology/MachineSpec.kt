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

package org.opendc.experiments.radice.scenario.topology

import com.fasterxml.jackson.annotation.JsonProperty
import org.opendc.simulator.compute.power.PowerModel

/**
 * A model for the machines in a cluster.
 *
 * @param id Identifier for machine model.
 * @param name Name of the machine model.
 * @param cpuCapacity The capacity of a CPU in the machine (in GHz)
 * @param cpuCount The number of physical CPUs in the cluster.
 * @param memCapacity The memory capacity of each machine (in GB)
 * @param powerModel The power model of the machine.
 */
data class MachineSpec(
    val id: String,
    val name: String,
    @JsonProperty("cpu-capacity")
    val cpuCapacity: Double,
    @JsonProperty("cpu-count")
    val cpuCount: Int,
    @JsonProperty("mem-capacity")
    val memCapacity: Double,
    @JsonProperty("power-model")
    val powerModel: PowerModel
) {
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is MachineSpec && id == other.id
}

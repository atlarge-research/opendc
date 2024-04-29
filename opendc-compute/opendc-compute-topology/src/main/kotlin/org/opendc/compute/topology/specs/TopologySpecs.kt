/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.topology.specs

import kotlinx.serialization.Serializable

/**
 * Definition of a Topology modeled in the simulation.
 *
 * @param clusters List of the clusters in this topology
 */
@Serializable
public data class TopologySpec(
    val clusters: List<ClusterSpec>,
    val schemaVersion: Int = 1,
)

/**
 * Definition of a compute cluster modeled in the simulation.
 *
 * @param name The name of the cluster.
 * @param hosts List of the different hosts (nodes) available in this cluster
 * @param location Location of the cluster. This can impact the carbon intensity
 */
@Serializable
public data class ClusterSpec(
    val name: String = "Cluster",
    val count: Int = 1,
    val hosts: List<HostJSONSpec>,
    val location: String = "NL",
)

/**
 * Definition of a compute host modeled in the simulation.
 *
 * @param name The name of the host.
 * @param cpu The CPU available in this cluster
 * @param memory The amount of RAM memory available in Byte
 * @param powerModel The power model used to determine the power draw of a host
 * @param count The power model used to determine the power draw of a host
 */
@Serializable
public data class HostJSONSpec(
    val name: String = "Host",
    val cpu: CPUSpec,
    val memory: MemorySpec,
    val powerModel: PowerModelSpec = PowerModelSpec("linear", 350.0, 400.0, 200.0),
    val count: Int = 1,
)

/**
 * Definition of a compute CPU modeled in the simulation.
 *
 * @param vendor The vendor of the storage device.
 * @param modelName The model name of the device.
 * @param arch The micro-architecture of the processor node.
 * @param coreCount The number of cores in the CPU
 * @param coreSpeed The speed of the cores in Mhz
 */
@Serializable
public data class CPUSpec(
    val vendor: String = "unknown",
    val modelName: String = "unknown",
    val arch: String = "unknown",
    val coreCount: Int,
    val coreSpeed: Double,
    val count: Int = 1,
)

/**
 * Definition of a compute Memory modeled in the simulation.
 *
 * @param vendor The vendor of the storage device.
 * @param modelName The model name of the device.
 * @param arch The micro-architecture of the processor node.
 * @param memorySpeed The speed of the cores in ?
 * @param memorySize The size of the memory Unit in MiB
 */
@Serializable
public data class MemorySpec(
    val vendor: String = "unknown",
    val modelName: String = "unknown",
    val arch: String = "unknown",
    val memorySpeed: Double = -1.0,
    val memorySize: Long,
)

@Serializable
public data class PowerModelSpec(
    val modelType: String,
    val power: Double = 400.0,
    val maxPower: Double,
    val idlePower: Double,
) {
    init {
        require(maxPower >= idlePower) { "The max power of a power model can not be less than the idle power" }
    }
}

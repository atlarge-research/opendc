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

@file:JvmName("TopologyFactories")

package org.opendc.compute.topology

import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.topology.specs.HostJSONSpec
import org.opendc.compute.topology.specs.HostSpec
import org.opendc.compute.topology.specs.TopologySpec
import org.opendc.simulator.compute.SimPsuFactories
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.getPowerModel
import java.io.File
import java.io.InputStream
import java.util.SplittableRandom
import java.util.UUID
import java.util.random.RandomGenerator

/**
 * A [TopologyReader] that is used to read the cluster definition file.
 */
private val reader = TopologyReader()

/**
 * Construct a topology from the specified [pathToFile].
 */
public fun clusterTopology(
    pathToFile: String,
    random: RandomGenerator = SplittableRandom(0),
): List<HostSpec> {
    return clusterTopology(File(pathToFile), random)
}

/**
 * Construct a topology from the specified [file].
 */
public fun clusterTopology(
    file: File,
    random: RandomGenerator = SplittableRandom(0),
): List<HostSpec> {
    val topology = reader.read(file)
    return topology.toHostSpecs(random)
}

/**
 * Construct a topology from the specified [input].
 */
public fun clusterTopology(
    input: InputStream,
    random: RandomGenerator = SplittableRandom(0),
): List<HostSpec> {
    val topology = reader.read(input)
    return topology.toHostSpecs(random)
}

/**
 * Helper method to convert a [TopologySpec] into a list of [HostSpec]s.
 */
private fun TopologySpec.toHostSpecs(random: RandomGenerator): List<HostSpec> {
    return clusters.flatMap { cluster ->
        List(cluster.count) {
            cluster.toHostSpecs(random)
        }.flatten()
    }
}

/**
 * Helper method to convert a [ClusterSpec] into a list of [HostSpec]s.
 */
private var clusterId = 0

private fun ClusterSpec.toHostSpecs(random: RandomGenerator): List<HostSpec> {
    val hostSpecs =
        hosts.flatMap { host ->
            (
                List(host.count) {
                    host.toHostSpecs(
                        clusterId,
                        random,
                    )
                }
            )
        }
    clusterId++
    return hostSpecs
}

/**
 * Helper method to convert a [HostJSONSpec] into a [HostSpec]s.
 */
private var hostId = 0
private var globalCoreId = 0

private fun HostJSONSpec.toHostSpecs(
    clusterId: Int,
    random: RandomGenerator,
): HostSpec {
    val unknownProcessingNode = ProcessingNode("unknown", "unknown", "unknown", cpu.coreCount)
    val units = List(cpu.count) { ProcessingUnit(unknownProcessingNode, globalCoreId++, cpu.coreSpeed.toMHz()) }

    val unknownMemoryUnit = MemoryUnit(memory.vendor, memory.modelName, memory.memorySpeed.toMHz(), memory.memorySize.toMB().toLong())
    val machineModel =
        MachineModel(
            units,
            listOf(unknownMemoryUnit),
        )

    val powerModel =
        getPowerModel(powerModel.modelType, powerModel.power.toWatts(), powerModel.maxPower.toWatts(), powerModel.idlePower.toWatts())

    var hostName: String
    if (name == null) {
        hostName = "Host-$hostId"
    } else {
        hostName = name
    }

    val hostSpec =
        HostSpec(
            UUID(random.nextLong(), (hostId).toLong()),
            hostName,
            mapOf("cluster" to clusterId),
            machineModel,
            SimPsuFactories.simple(powerModel),
        )
    hostId++

    return hostSpec
}

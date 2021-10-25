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
package org.opendc.experiments.capelin.topology

import org.opendc.compute.workload.topology.HostSpec
import org.opendc.compute.workload.topology.Topology
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.LinearPowerModel
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.math.roundToLong

/**
 * A [ClusterSpecReader] that is used to read the cluster definition file.
 */
private val reader = ClusterSpecReader()

/**
 * Construct a [Topology] from the specified [file].
 */
fun clusterTopology(
    file: File,
    powerModel: PowerModel = LinearPowerModel(350.0, idlePower = 200.0),
    random: Random = Random(0)
): Topology = clusterTopology(reader.read(file), powerModel, random)

/**
 * Construct a [Topology] from the specified [input].
 */
fun clusterTopology(
    input: InputStream,
    powerModel: PowerModel = LinearPowerModel(350.0, idlePower = 200.0),
    random: Random = Random(0)
): Topology = clusterTopology(reader.read(input), powerModel, random)

/**
 * Construct a [Topology] from the given list of [clusters].
 */
fun clusterTopology(
    clusters: List<ClusterSpec>,
    powerModel: PowerModel,
    random: Random = Random(0)
): Topology {
    return object : Topology {
        override fun resolve(): List<HostSpec> {
            val hosts = mutableListOf<HostSpec>()
            for (cluster in clusters) {
                val cpuSpeed = cluster.cpuSpeed
                val memoryPerHost = cluster.memCapacityPerHost.roundToLong()

                val unknownProcessingNode = ProcessingNode("unknown", "unknown", "unknown", cluster.cpuCountPerHost)
                val unknownMemoryUnit = MemoryUnit("unknown", "unknown", -1.0, memoryPerHost)
                val machineModel = MachineModel(
                    List(cluster.cpuCountPerHost) { coreId -> ProcessingUnit(unknownProcessingNode, coreId, cpuSpeed) },
                    listOf(unknownMemoryUnit)
                )

                repeat(cluster.hostCount) {
                    val spec = HostSpec(
                        UUID(random.nextLong(), it.toLong()),
                        "node-${cluster.name}-$it",
                        mapOf("cluster" to cluster.id),
                        machineModel,
                        SimplePowerDriver(powerModel)
                    )

                    hosts += spec
                }
            }

            return hosts
        }

        override fun toString(): String = "ClusterSpecTopology"
    }
}

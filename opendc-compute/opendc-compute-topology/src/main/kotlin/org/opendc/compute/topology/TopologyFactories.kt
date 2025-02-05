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

import org.opendc.compute.topology.specs.BatterySpec
import org.opendc.compute.topology.specs.ClusterJSONSpec
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.topology.specs.HostJSONSpec
import org.opendc.compute.topology.specs.HostSpec
import org.opendc.compute.topology.specs.PowerSourceSpec
import org.opendc.compute.topology.specs.TopologySpec
import org.opendc.simulator.compute.cpu.getPowerModel
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import java.io.File
import java.io.InputStream

/**
 * A [TopologyReader] that is used to read the cluster definition file.
 */
private val reader = TopologyReader()

// Lists used to make sure all cluster, host, power source and battery have unique names
private val clusterNames: ArrayList<String> = ArrayList()
private val hostNames: ArrayList<String> = ArrayList()
private val powerSourceNames: ArrayList<String> = ArrayList()
private val batteryNames: ArrayList<String> = ArrayList()

/**
 * Create a unique name for the specified [name] that is not already in the [names] list.
 *
 * If [name] is already in [names], "-$i" is appended to the name until a unique name is found.
 * In which "$i" is an increasing integer starting from 0.
 */
private fun createUniqueName(
    name: String,
    names: ArrayList<String>,
): String {
    if (name !in names) {
        names.add(name)
        return name
    }

    var i = 0
    var newName = "$name-$i"
    while (newName in names) {
        newName = "$name-${++i}"
    }

    names.add(newName)
    return newName
}

/**
 * Construct a topology from the specified [pathToFile].
 */
public fun clusterTopology(pathToFile: String): List<ClusterSpec> {
    return clusterTopology(File(pathToFile))
}

/**
 * Construct a topology from the specified [file].
 */
public fun clusterTopology(file: File): List<ClusterSpec> {
    val topology = reader.read(file)
    return topology.toClusterSpec()
}

/**
 * Construct a topology from the specified [input].
 */
public fun clusterTopology(input: InputStream): List<ClusterSpec> {
    val topology = reader.read(input)
    return topology.toClusterSpec()
}

/**
 * Helper method to convert a [TopologySpec] into a list of [HostSpec]s.
 */
private fun TopologySpec.toClusterSpec(): List<ClusterSpec> {
    clusterNames.clear()
    hostNames.clear()
    powerSourceNames.clear()
    batteryNames.clear()

    return clusters.map { cluster ->
        cluster.toClusterSpec()
    }
}

/**
 * Helper method to convert a [ClusterJSONSpec] into a list of [HostSpec]s.
 */
private fun ClusterJSONSpec.toClusterSpec(): ClusterSpec {
    val clusterName = createUniqueName(this.name, clusterNames)

    val hostSpecs =
        hosts.flatMap { host ->
            (
                List(host.count) {
                    host.toHostSpec(
                        clusterName,
                    )
                }
            )
        }
    val powerSourceSpec =
        PowerSourceSpec(
            createUniqueName(this.powerSource.name, powerSourceNames),
            totalPower = this.powerSource.totalPower,
            carbonTracePath = this.powerSource.carbonTracePath,
        )

    var batterySpec: BatterySpec? = null
    if (this.battery != null) {
        batterySpec =
            BatterySpec(
                createUniqueName(this.battery.name, batteryNames),
                this.battery.capacity,
                this.battery.chargingSpeed,
                this.battery.batteryPolicy,
                this.battery.initialCharge,
            )
    }

    return ClusterSpec(clusterName, hostSpecs, powerSourceSpec, batterySpec)
}

/**
 * Helper method to convert a [HostJSONSpec] into a [HostSpec]s.
 */
private var globalCoreId = 0

private fun HostJSONSpec.toHostSpec(clusterName: String): HostSpec {
    val units =
        List(cpu.count) {
            CpuModel(
                globalCoreId++,
                cpu.coreCount,
                cpu.coreSpeed.toMHz(),
            )
        }

    val unknownMemoryUnit = MemoryUnit(memory.vendor, memory.modelName, memory.memorySpeed.toMHz(), memory.memorySize.toMiB().toLong())
    val machineModel =
        MachineModel(
            units,
            unknownMemoryUnit,
        )

    val powerModel =
        getPowerModel(powerModel.modelType, powerModel.power.toWatts(), powerModel.maxPower.toWatts(), powerModel.idlePower.toWatts())

    val hostSpec =
        HostSpec(
            createUniqueName(this.name, hostNames),
            clusterName,
            machineModel,
            powerModel,
        )
    return hostSpec
}

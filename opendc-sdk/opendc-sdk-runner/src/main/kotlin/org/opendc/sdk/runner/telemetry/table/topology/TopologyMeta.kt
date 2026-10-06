/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.runner.telemetry.table.topology

import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.service.ComputeService
import org.opendc.trace.parquet.exporter.Exportable

/**
 * The topology of a run. Samples only carry the id of the entity they describe and what changes during the run, so the
 * topology is exported once per run to map those ids to names and static attributes, and to tell which host is part of
 * which cluster and which cluster, power source and battery is part of which data center.
 */
public data class TopologyMeta(
    public val dataCenters: List<DataCenterMeta> = emptyList(),
    public val clusters: List<ClusterMeta> = emptyList(),
    public val hosts: List<HostMeta> = emptyList(),
    public val powerSources: List<PowerSourceMeta> = emptyList(),
    public val batteries: List<BatteryMeta> = emptyList(),
) {
    public companion object {
        /**
         * Capture the topology provisioned for [service], ordered by id.
         */
        public fun of(service: ComputeService): TopologyMeta =
            TopologyMeta(
                dataCenters = service.dataCenters.map { DataCenterMeta(it.id, it.getName()) }.sortedBy { it.dataCenterId },
                clusters = service.clusters.map { ClusterMeta(it.id, it.getName(), it.dataCenterId) }.sortedBy { it.clusterId },
                hosts = service.hosts.map { HostMeta.of(it) }.sortedBy { it.hostId },
                powerSources =
                    service.powerSources.map { PowerSourceMeta(it.id, it.name, it.dataCenterId) }.sortedBy { it.powerSourceId },
                batteries =
                    service.batteries.map { BatteryMeta(it.id, it.name, it.dataCenterId, it.capacity) }.sortedBy { it.batteryId },
            )
    }
}

public data class DataCenterMeta(
    public val dataCenterId: Int,
    public val dataCenterName: String,
) : Exportable

public data class ClusterMeta(
    public val clusterId: Int,
    public val clusterName: String,
    public val dataCenterId: Int,
) : Exportable

/**
 * @property cpuCapacity The total CPU capacity of the host in MHz.
 * @property memCapacity The memory of the host in MiB.
 * @property gpuCapacities The capacity of each GPU of the host in MHz.
 */
public data class HostMeta(
    public val hostId: Int,
    public val hostName: String,
    public val clusterId: Int,
    public val coreCount: Int,
    public val cpuCapacity: Double,
    public val memCapacity: Long,
    public val gpuCapacities: List<Double> = emptyList(),
) : Exportable {
    public companion object {
        public fun of(host: SimHost): HostMeta =
            HostMeta(
                hostId = host.id,
                hostName = host.name,
                clusterId = host.clusterId,
                coreCount = host.model.coreCount,
                cpuCapacity = host.model.cpuCapacity,
                memCapacity = host.model.memoryCapacity,
                gpuCapacities = host.model.gpuHostModels.orEmpty().map { it.gpuCoreCapacity },
            )
    }
}

public data class PowerSourceMeta(
    public val powerSourceId: Int,
    public val powerSourceName: String,
    public val dataCenterId: Int,
) : Exportable

/**
 * @property capacity The capacity of the battery in J.
 */
public data class BatteryMeta(
    public val batteryId: Int,
    public val batteryName: String,
    public val dataCenterId: Int,
    public val capacity: Double,
) : Exportable

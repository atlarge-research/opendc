/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.dsl

import org.opendc.common.units.Power
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.topology.BatteryPolicySpec
import org.opendc.sdk.model.topology.BatterySpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.PowerSourceSpec
import org.opendc.sdk.model.topology.TopologySpec

/**
 * Builds a [TopologySpec] from one or more clusters.
 *
 * @param block Configures the topology through a [TopologyBuilder].
 */
public fun topology(block: TopologyBuilder.() -> Unit): TopologySpec = TopologyBuilder().apply(block).build()

/** Collects the clusters composing a [TopologySpec]. */
@SdkDsl
public class TopologyBuilder {
    private val datacenters = mutableListOf<DataCenterSpec>()

    public fun datacenter(
        name: String = "DataCenter",
        count: Int = 1,
        block: DataCenterBuilder.() -> Unit,
    ) {
        datacenters += DataCenterBuilder(name, count).apply(block).build()
    }

    internal fun build(): TopologySpec = TopologySpec(datacenters=datacenters)
}

/** Collects the hosts, power source, and optional battery of a [ClusterSpec]. */
@SdkDsl
public class DataCenterBuilder(private val name: String, private val count: Int) {
    private val clusters = mutableListOf<ClusterSpec>()
    private var powerSource: PowerSourceSpec = PowerSourceSpec()
    private var battery: BatterySpec? = null

    public fun cluster(
        count: Int = 1,
        name: String = "Host",
        block: ClusterBuilder.() -> Unit,
    ) {
        clusters += ClusterBuilder(name, count).apply(block).build()
    }

    public fun powerSource(
        name: String = "PowerSource",
        maxPower: Power = Power.ofWatts(Long.MAX_VALUE.toDouble()),
        carbon: ResourceReference? = null,
    ) {
        powerSource = PowerSourceSpec(name, maxPower, carbon)
    }

    public fun battery(
        capacity: Double,
        chargingSpeed: Double,
        policy: BatteryPolicySpec,
        name: String = "Battery",
        initialCharge: Double = 0.0,
        embodiedCarbon: Double = 0.0,
        expectedLifetime: Double = 0.0,
    ) {
        battery = BatterySpec(name, capacity, chargingSpeed, initialCharge, policy, embodiedCarbon, expectedLifetime)
    }

    internal fun build(): DataCenterSpec = DataCenterSpec(clusters.toList(), name, powerSource, battery)
}


/** Collects the hosts, power source, and optional battery of a [ClusterSpec]. */
@SdkDsl
public class ClusterBuilder(private val name: String, private val count: Int) {
    private val hosts = mutableListOf<HostSpec>()

    public fun host(
        count: Int = 1,
        name: String = "Host",
        block: HostBuilder.() -> Unit,
    ) {
        hosts += HostBuilder(name, count).apply(block).build()
    }

    internal fun build(): ClusterSpec = ClusterSpec(name, count, hosts.toList())
}

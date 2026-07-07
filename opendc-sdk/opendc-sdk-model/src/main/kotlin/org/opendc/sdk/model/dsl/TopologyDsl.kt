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
import org.opendc.sdk.model.topology.Battery
import org.opendc.sdk.model.topology.BatteryPolicy
import org.opendc.sdk.model.topology.Cluster
import org.opendc.sdk.model.topology.Host
import org.opendc.sdk.model.topology.PowerSource
import org.opendc.sdk.model.topology.Topology

/**
 * Builds a [Topology] from one or more clusters.
 *
 * @param block Configures the topology through a [TopologyBuilder].
 */
public fun topology(block: TopologyBuilder.() -> Unit): Topology = TopologyBuilder().apply(block).build()

/** Collects the clusters composing a [Topology]. */
@SdkDsl
public class TopologyBuilder {
    private val clusters = mutableListOf<Cluster>()

    public fun cluster(
        name: String = "Cluster",
        count: Int = 1,
        block: ClusterBuilder.() -> Unit,
    ) {
        clusters += ClusterBuilder(name, count).apply(block).build()
    }

    internal fun build(): Topology = Topology(clusters.toList())
}

/** Collects the hosts, power source, and optional battery of a [Cluster]. */
@SdkDsl
public class ClusterBuilder(private val name: String, private val count: Int) {
    private val hosts = mutableListOf<Host>()
    private var powerSource: PowerSource = PowerSource()
    private var battery: Battery? = null

    public fun host(
        count: Int = 1,
        name: String = "Host",
        block: HostBuilder.() -> Unit,
    ) {
        hosts += HostBuilder(name, count).apply(block).build()
    }

    public fun powerSource(
        name: String = "PowerSource",
        maxPower: Power = Power.ofWatts(Long.MAX_VALUE.toDouble()),
        carbon: ResourceReference? = null,
    ) {
        powerSource = PowerSource(name, maxPower, carbon)
    }

    public fun battery(
        capacity: Double,
        chargingSpeed: Double,
        policy: BatteryPolicy,
        name: String = "Battery",
        initialCharge: Double = 0.0,
        embodiedCarbon: Double = 0.0,
        expectedLifetime: Double = 0.0,
    ) {
        battery = Battery(name, capacity, chargingSpeed, initialCharge, policy, embodiedCarbon, expectedLifetime)
    }

    internal fun build(): Cluster = Cluster(name, count, hosts.toList(), powerSource, battery)
}
